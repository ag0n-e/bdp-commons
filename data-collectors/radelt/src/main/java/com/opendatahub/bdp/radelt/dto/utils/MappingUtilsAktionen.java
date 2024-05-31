package it.bz.idm.bdp.radelt.dto.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import java.util.ArrayList;
import java.time.ZoneOffset;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;


import it.bz.idm.bdp.radelt.dto.aktionen.AktionenResponseDto;
import it.bz.idm.bdp.radelt.dto.aktionen.RadeltChallengeDto;
import it.bz.idm.bdp.radelt.dto.aktionen.RadeltStatisticDto;
import it.bz.idm.bdp.radelt.dto.aktionen.RadeltChallengeMetric;

import it.bz.idm.bdp.radelt.dto.utils.DataTypeUtils;
import it.bz.idm.bdp.radelt.OdhClient;
import org.slf4j.Logger;

public class MappingUtilsAktionen {

	public static final String DATA_ORIGIN = "SuedtirolRadelt_AltoAdigePedala";
	public static final String DATA_TYPE = "GamificationAction";

	public static StationDto mapToStationDto(RadeltChallengeDto challengeDto) {
		StationDto stationDto = new StationDto();
		stationDto.setId(String.valueOf(challengeDto.getId()));
		stationDto.setName(challengeDto.getName());

		//METADATA
		stationDto.getMetaData().put("shortName", challengeDto.getShortName());
		stationDto.getMetaData().put("Slug", SlugUtils.getSlug(challengeDto.getShortName()));
		stationDto.getMetaData().put("headerImage", challengeDto.getHeaderImage());
		stationDto.getMetaData().put("Start", challengeDto.getStart());
		stationDto.getMetaData().put("End", challengeDto.getEnd());
		stationDto.getMetaData().put("registrationStart", challengeDto.getRegistrationStart());
		stationDto.getMetaData().put("registrationEnd", challengeDto.getRegistrationEnd());
		stationDto.getMetaData().put("type", challengeDto.getType());
		stationDto.getMetaData().put("isExternal", challengeDto.isExternal());
		stationDto.getMetaData().put("canOrganisationsSignup", challengeDto.isCanOrganisationsSignup());

		//Additional
		stationDto.setOrigin(DATA_ORIGIN);
		//TODO: set pointprojection from csv, latitude and longitude? Prepare an example id, latitude, longitude
		stationDto.setStationType(DATA_TYPE);

		return stationDto;
	}

	public static void mapToStationList(AktionenResponseDto responseDto, OdhClient odhClient, Logger LOG) {
		StationList stationListAktionen = new StationList();
		List<DataTypeDto> odhDataTypeList = new ArrayList<>();

		for (RadeltChallengeDto challengeDto : responseDto.getData().getChallenges()) {
			//Create Station
			StationDto stationDto = mapToStationDto(challengeDto);

			if(stationDto.getId() != null){//TODO: handle duplicated entries?
				stationListAktionen.add(stationDto);
				LOG.info("Add station with id #" + stationDto.getId());
			}

			// Create measurement records
			DataMapDto<RecordDtoImpl> dataMap = new DataMapDto<>();
			DataMapDto<RecordDtoImpl> stationMap = dataMap.upsertBranch(stationDto.getId());

			RadeltStatisticDto statistics = challengeDto.getStatistics();

			if(statistics != null && statistics.getChallenge() != null){
				RadeltChallengeMetric challenge = statistics.getChallenge();
				long timestamp = challenge.getCreated_at().toInstant(ZoneOffset.UTC).toEpochMilli();

				DataTypeUtils.addMeasurement(stationMap, "km_total", timestamp, challenge.getKm_total());
				DataTypeUtils.addMeasurement(stationMap, "height_meters_total", timestamp, challenge.getHeight_meters_total());
				DataTypeUtils.addMeasurement(stationMap, "km_average", timestamp, challenge.getKm_average());
				DataTypeUtils.addMeasurement(stationMap, "kcal", timestamp, challenge.getKcal());
				DataTypeUtils.addMeasurement(stationMap, "co2", timestamp, challenge.getCo2());
				DataTypeUtils.addMeasurement(stationMap, "m2_trees", timestamp, challenge.getM2_trees());
				DataTypeUtils.addMeasurement(stationMap, "money_saved", timestamp, challenge.getMoney_saved());
				DataTypeUtils.addMeasurement(stationMap, "number_of_people", timestamp, challenge.getNumber_of_people());
				DataTypeUtils.addMeasurement(stationMap, "organisation_count", timestamp, challenge.getOrganisation_count());
				DataTypeUtils.addMeasurement(stationMap, "workplace_count", timestamp, challenge.getWorkplace_count());
				DataTypeUtils.addMeasurement(stationMap, "school_count", timestamp, challenge.getSchool_count());
				DataTypeUtils.addMeasurement(stationMap, "municipality_count", timestamp, challenge.getMunicipality_count());
				DataTypeUtils.addMeasurement(stationMap, "association_count", timestamp, challenge.getAssociation_count());
				DataTypeUtils.addMeasurement(stationMap, "university_count", timestamp, challenge.getUniversity_count());

				// Push data
				try {
					odhClient.pushData(stationMap);
					LOG.info("Pushing data successful");
				} catch (WebClientRequestException e) {
					LOG.error("Pushing data failed: Request exception: {}", e.getMessage());
				}
			}
		}

		// Sync stations
		try {
			odhClient.syncStations(stationListAktionen);
			LOG.info("Syncing stations successful");
		} catch (WebClientRequestException e) {
			LOG.error("Syncing stations failed: Request exception: {}", e.getMessage());
		}
	}
}
