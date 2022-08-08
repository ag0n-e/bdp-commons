package it.bz.odh.trafficprovbz;

import com.jayway.jsonpath.JsonPath;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.odh.trafficprovbz.dto.AggregatedDataDto;
import it.bz.odh.trafficprovbz.dto.MetadataDto;
import it.bz.odh.trafficprovbz.dto.PassagesDataDto;
import net.minidev.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Parser {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	/**
	 * This is a function to create a station, either of type traffic or bluetooth
	 *
	 * @author Peter Schweigkofler
	 *
	 * @param metadataDto where the station is stored
	 * @param otherFields is a jsonobject where all other fields that Jackson did not match are stored
	 * @param lane is a param set if it´s a traffic sensor and viceversa for bluetooth sensor containing the info of the lane
	 * @param classificationSchema is a hash map where the schema is stored
	 * @param stationType is a string either of type traffic or bluetooth
	 * @return station containing the data from the params
	 */
	public static StationDto createStation(MetadataDto metadataDto, JSONObject otherFields, LinkedHashMap<String, String> lane, LinkedHashMap<String, String> classificationSchema, String stationType) {
		Double lat = JsonPath.read(otherFields, "$.GeoInfo.Latitudine");
		Double lon = JsonPath.read(otherFields, "$.GeoInfo.Longitudine");

		metadataDto.setOtherField("SchemaDiClassificazione", classificationSchema);
		String stationId = metadataDto.getId();
		String stationName = metadataDto.getName();
		if (lane != null) {
			String description = JsonPath.read(lane, "$.Descrizione");
			stationId = metadataDto.getId() + ":" + description;
			stationName = metadataDto.getName() + ":" + description;
		}
		StationDto station = new StationDto(stationId, stationName, lat, lon);
		station.setStationType(stationType);
		return station;
	}

	public static void insertDataIntoStationMap(AggregatedDataDto[] aggregatedDataDtos, Integer period, DataMapDto<RecordDtoImpl> stationMap) throws ParseException {

		List<String> trafficDataTypes = Parser.getTrafficDataTypes();

		@SuppressWarnings("unchecked")
		DataMapDto<RecordDtoImpl>[] metricMaps = new DataMapDto[trafficDataTypes.size()];

		for (int i = 0; i < trafficDataTypes.size(); i++) {
			DataMapDto<RecordDtoImpl> metricMap = stationMap.upsertBranch(trafficDataTypes.get(i));
			metricMaps[i] = metricMap;
		}

		for (AggregatedDataDto aggregatedDataDto : aggregatedDataDtos) {
			Long timestamp = formatter.parse(aggregatedDataDto.getDate()).getTime();
			JSONObject otherFields = new JSONObject(aggregatedDataDto.getOtherFields());
			addMeasurementToMap(metricMaps[0], new SimpleRecordDto(timestamp, aggregatedDataDto.getTotalTransits(), period));
			if (otherFields.containsKey("TotaliPerClasseVeicolare")) {
				LinkedHashMap<String, Integer> classes = JsonPath.read(otherFields, "$.TotaliPerClasseVeicolare");
				Set<String> keys = classes.keySet();
				for (String key : keys) {
					for (int i = 1; i <= 10; i++) {
						if (String.valueOf(i).equals(key)) {
							addMeasurementToMap(metricMaps[i], new SimpleRecordDto(timestamp, classes.get(key), period));
						}
					}
				}
				addMeasurementToMap(metricMaps[11], new SimpleRecordDto(timestamp, aggregatedDataDto.getAverageVehicleSpeed(), period));
				addMeasurementToMap(metricMaps[12], new SimpleRecordDto(timestamp, aggregatedDataDto.getHeadway(), period));
				addMeasurementToMap(metricMaps[13], new SimpleRecordDto(timestamp, aggregatedDataDto.getHeadwayVariance(), period));
				addMeasurementToMap(metricMaps[14], new SimpleRecordDto(timestamp, aggregatedDataDto.getGap(), period));
				addMeasurementToMap(metricMaps[15], new SimpleRecordDto(timestamp, aggregatedDataDto.getGapVariance(), period));
			}
		}
	}

	private static List<String> getTrafficDataTypes() {
		return Arrays.asList("total-transits", "number-of-motorcycles", "number-of-cars", "number-of-cars-and-minivans-with-trailer", "number-of-small-trucks-and-vans ",
			"number-of-medium-sized-trucks", "number-of-big-trucks", "number-of-articulated-trucks", "number-of-articulated-lorries", "number-of-busses",
			"number-of-unclassified-vehicles", "average-vehicle-speed", "headway", "headway-variance", "gap", "gap-variance");
	}

	public static void insertDataIntoBluetoothmap(PassagesDataDto[] passagesDataDtos, Integer period, DataMapDto<RecordDtoImpl> bluetoothMetricMap) throws ParseException {
		for (PassagesDataDto passagesDataDto : passagesDataDtos) {
			Long timestamp = formatter.parse(passagesDataDto.getDate()).getTime();
			addMeasurementToMap(bluetoothMetricMap, new SimpleRecordDto(timestamp, passagesDataDto.getIdVehicle(), period));
		}
	}

	private static void addMeasurementToMap(DataMapDto<RecordDtoImpl> map, SimpleRecordDto measurement) {
		if (map != null) {
			measurement.setCreated_on(new Date().getTime());
			map.getData().add(measurement);
		}
	}
}
