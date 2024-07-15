// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.opendatahub.bdp.radelt.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import java.util.ArrayList;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

import com.opendatahub.bdp.radelt.dto.aktionen.AktionenResponseDto;
import com.opendatahub.bdp.radelt.dto.aktionen.RadeltChallengeDto;
import com.opendatahub.bdp.radelt.dto.aktionen.RadeltStatisticDto;
import com.opendatahub.bdp.radelt.dto.aktionen.RadeltChallengeMetric;
import com.opendatahub.bdp.radelt.dto.common.RadeltGeoDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class MappingUtilsAktionen {

	private final Logger LOG = LoggerFactory.getLogger(MappingUtilsAktionen.class);

	public final String DATA_ORIGIN = "SuedtirolRadelt_AltoAdigePedala";
	public final String DATA_TYPE = "GamificationAction";

	@Value("${odh_client.period}")
	private Integer period;

	public void mapData(AktionenResponseDto responseDto, Map<String, RadeltGeoDto> actionCoordinates,
			StationList stationList,
			DataMapDto<RecordDtoImpl> rootMap) {

		for (RadeltChallengeDto challengeDto : responseDto.getData().getChallenges()) {
			// Create Station
			StationDto stationDto = mapToStationDto(challengeDto, actionCoordinates);

			if (stationDto.getId() == null) {// TODO: handle duplicated entries?
				LOG.debug("Skipping station with empty id");
				continue;
			}

			stationList.add(stationDto);

			RadeltStatisticDto statistics = challengeDto.getStatistics();

			if (statistics != null && statistics.getChallenge() != null) {
				RadeltChallengeMetric challenge = statistics.getChallenge();
				long timestamp = challenge.getCreated_at();

				rootMap.addRecord(stationDto.getId(), "km_total",
						new SimpleRecordDto(timestamp, challenge.getKm_total(), period));
				rootMap.addRecord(stationDto.getId(), "height_meters_total",
						new SimpleRecordDto(timestamp, challenge.getHeight_meters_total(), period));
				rootMap.addRecord(stationDto.getId(), "km_average",
						new SimpleRecordDto(timestamp, challenge.getKm_average(), period));
				rootMap.addRecord(stationDto.getId(), "kcal",
						new SimpleRecordDto(timestamp, challenge.getKcal(), period));
				rootMap.addRecord(stationDto.getId(), "co2",
						new SimpleRecordDto(timestamp, challenge.getCo2(), period));
				rootMap.addRecord(stationDto.getId(), "m2_trees",
						new SimpleRecordDto(timestamp, challenge.getM2_trees(), period));
				rootMap.addRecord(stationDto.getId(), "money_saved",
						new SimpleRecordDto(timestamp, challenge.getMoney_saved(), period));
				rootMap.addRecord(stationDto.getId(), "number_of_people", new SimpleRecordDto(timestamp,
						challenge.getNumber_of_people(), period));
				rootMap.addRecord(stationDto.getId(), "organisation_count", new SimpleRecordDto(timestamp,
						challenge.getOrganisation_count(), period));
				rootMap.addRecord(stationDto.getId(), "workplace_count",
						new SimpleRecordDto(timestamp, challenge.getWorkplace_count(), period));
				rootMap.addRecord(stationDto.getId(), "school_count",
						new SimpleRecordDto(timestamp, challenge.getSchool_count(), period));
				rootMap.addRecord(stationDto.getId(), "municipality_count", new SimpleRecordDto(timestamp,
						challenge.getMunicipality_count(), period));
				rootMap.addRecord(stationDto.getId(), "association_count", new SimpleRecordDto(timestamp,
						challenge.getAssociation_count(), period));
				rootMap.addRecord(stationDto.getId(), "university_count", new SimpleRecordDto(timestamp,
						challenge.getUniversity_count(), period));
			}
		}

	}

	private StationDto mapToStationDto(RadeltChallengeDto challengeDto, Map<String, RadeltGeoDto> actionCoordinates) {
		StationDto stationDto = new StationDto();
		stationDto.setId(String.valueOf(challengeDto.getId()));
		stationDto.setName(challengeDto.getName());

		// METADATA
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

		// Additional
		stationDto.setOrigin(DATA_ORIGIN);
		// Geo info from csv
		RadeltGeoDto actionGeoDto = actionCoordinates.get(stationDto.getId());
		if (actionGeoDto != null) {
			stationDto.setLongitude(actionGeoDto.getLongitude());
			stationDto.setLatitude(actionGeoDto.getLatitude());
			LOG.info("Coordinates saved for station: {}, lat: {} lon: {}", stationDto.getId(),
					actionGeoDto.getLatitude(), actionGeoDto.getLongitude());
		} else {
			LOG.debug("No coordinates found on csv for station with id: #" + stationDto.getId());
		}
		stationDto.setStationType(DATA_TYPE);

		return stationDto;
	}
}
