// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.opendatahub.bdp.commons.dc.bikeboxes;

import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.opendatahub.bdp.commons.dc.bikeboxes.config.DataConfig;
import com.opendatahub.bdp.commons.dc.bikeboxes.config.DataTypes;
import com.opendatahub.bdp.commons.dc.bikeboxes.config.ProvenanceConfig;
import com.opendatahub.bdp.commons.dc.bikeboxes.config.StationConfig;
import com.opendatahub.bdp.commons.dc.bikeboxes.dto.BikeStation;
import com.opendatahub.bdp.commons.dc.bikeboxes.dto.BikeStation.Place;
import com.opendatahub.bdp.commons.dc.bikeboxes.services.BikeBoxesService;
import com.opendatahub.bdp.commons.dc.bikeboxes.services.OdhClient;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

@Service
public class BikeBoxJobScheduler {
	private static final Logger LOG = LoggerFactory.getLogger(BikeBoxJobScheduler.class);

	@Lazy
	@Autowired
	private OdhClient odhClient;

	@Lazy
	@Autowired
	private BikeBoxesService bikeBoxesService;

	@Autowired
	private DataConfig dataC;

	@Autowired
	private StationConfig stationC;

	@Autowired
	private ProvenanceConfig provC;

	@Scheduled(cron = "${scheduler.job}")
	public void collectBikeBoxData() {
		LOG.info("Cron job started");

		try {
			LOG.debug("Getting stations");
			List<BikeStation> bikeStations = bikeBoxesService.getBikeStations();

			if (LOG.isTraceEnabled()) {
				LOG.trace("Dumping retrieved stations list:");
				bikeStations.stream().forEach(s -> LOG.trace(s.toString()));
			}

			LOG.debug("Mapping to ODH objects");
			StationList odhStations = new StationList();
			StationList odhBays = new StationList();
			DataMapDto<RecordDtoImpl> odhData = new DataMapDto<>();
			DataMapDto<RecordDtoImpl> odhBayData = new DataMapDto<>();

			for (BikeStation bs : bikeStations) {
				// create station dto
				StationDto stationDto = new StationDto(Integer.toString(bs.stationID), bs.locationName, bs.latitude,
						bs.longitude);

				stationDto.setMetaData(Map.of(
						"type", mapBikeStationType(bs.type),
						"totalPlaces", bs.totalPlaces,
						"locationID", bs.locationID,
						"names", bs.locationNames,
						"addresses", bs.addresses,
						"stationPlaces", Arrays.stream(bs.places).map(p -> Map.of(
								"position", p.position,
								// purposely don't include state field
								"type", mapBikeStationBayType(p.type),
								"level", p.level))));
				stationDto.setOrigin(provC.origin);
				odhStations.add(stationDto);

				// create station level measurements (as key value pairs)
				var stationData = Map.of(
						DataTypes.usageState.key, mapBikeStationBayState(bs.state),
						DataTypes.freeSpotsNonEbikes.key, bs.countFreePlacesAvailable_MuscularBikes,
						DataTypes.freeSpotsEbikes.key, bs.countFreePlacesAvailable_AssistedBikes,
						DataTypes.free.key, bs.countFreePlacesAvailable);
				// add the created measurements to odh data list
				stationData.forEach((t, v) -> odhData.addRecord(stationDto.getId(), t, mapSimple(v)));

				// create station and measurement for sub stations (parking bays)
				for (Place bay : bs.places) {
					StationDto bayDto = new StationDto(
							stationDto.getId() + "_" + bs.stationID + "/" + bay.position,
							stationDto.getName() + "/" + bay.position,
							stationDto.getLatitude(),
							stationDto.getLongitude());
					bayDto.setOrigin(stationDto.getOrigin());
					// this parking bay is a child of the parking station
					bayDto.setParentStation(stationDto.getId());

					bayDto.getMetaData().put(
							"type", mapBikeStationBayType(bay.type));
					bayDto.getMetaData().put("position", bay.position);
					bayDto.getMetaData().put("level", bay.level);

					odhBays.add(bayDto);

					// add bay level measurement
					odhBayData.addRecord(bayDto.getId(), DataTypes.usageState.key, mapSimple(mapUsageState(bay.state)));
				}
			}

			LOG.debug("Pushing data to ODH");
			odhClient.syncDataTypes(stationC.stationBayType,
					Arrays.stream(DataTypes.values())
							.filter(d -> d.syncToOdh)
							.map(DataTypes::toDataTypeDto)
							.toList());
			odhClient.syncStations(stationC.stationType, odhStations, 25);
			odhClient.syncStations(stationC.stationBayType, odhBays, 25);
			odhClient.pushData(stationC.stationType, odhData);
			odhClient.pushData(stationC.stationBayType, odhBayData);
			LOG.info("Cron job successful");
		} catch (Exception e) {
			LOG.error("Cron job failed: exception: {}", e.getMessage(), e);
		}
	}

	private String mapBikeStationType(int type) {
		return switch (type) {
			case 4 -> "veloHub";
			case 5 -> "bixeBoxGroup";
			default -> "unknown";
		};
	}

	private String mapBikeStationBayType(int type) {
		return switch (type) {
			case 1 -> "withoutRefill";
			case 2 -> "withRefill";
			default -> "unknown";
		};
	}

	private String mapBikeStationBayState(int type) {
		return switch (type) {
			case 1 -> "in service";
			case 2 -> "out of service";
			default -> "unknown";
		};
	}

	private String mapUsageState(int state) {
		return switch (state) {
			case 1 -> "in service";
			case 2 -> "occupied - in service";
			case 3 -> "out of service";
			default -> "unknown";
		};
	}

	private SimpleRecordDto mapSimple(Object value) {
		return new SimpleRecordDto(System.currentTimeMillis(), value, dataC.period);
	}
}
