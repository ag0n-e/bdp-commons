package it.bz.odh.trafficprovbz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.json.parser.JSONParser;
import com.nimbusds.jose.shaded.json.parser.ParseException;
import it.bz.odh.trafficprovbz.dto.AggregatedDataDto;
import it.bz.odh.trafficprovbz.dto.ClassificationSchemaDto;
import it.bz.odh.trafficprovbz.dto.MetadataDto;
import it.bz.odh.trafficprovbz.dto.PassagesDataDto;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;

@Lazy
@Service
public class FamasClient {
	private static final String RESPONSE_CHARSET = "UTF-8";

	private static final String STATION_ID_URL_PARAM = "%STATION_ID%";

	@Value("${endpoint.classificationSchemas.url}")
	private String classificationSchemasUrl;

	@Value("${endpoint.stationsData.url}")
	private String stationsDataUrl;

	@Value("${endpoint.aggregatedDataOnStations.url}")
	private String aggregatedDataOnStationsUrl;

	@Value("${endpoint.passageDataOnStations.url}")
	private String passagesDataOnStationsUrl;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final HttpClient client = HttpClientBuilder.create().build();

	/**
	 * This function gets all the classification schemas via an api from famas
	 *
	 * @return ClassificationSchemaDto-array with all the classification schemas
	 * @throws IOException used in the code to throw a failure in input and output operations
	 */
	public ClassificationSchemaDto[] getClassificationSchemas() throws IOException {
		// TODO: Comment out code and remove test json files if api is available
		//HttpResponse response = client.execute(new HttpGet(classificationSchemasUrl));
		//HttpEntity entity = response.getEntity();
		//String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		String responseString = readJsonFile("jsonfiles/classificationSchemas.json");
		return objectMapper.readValue(responseString, ClassificationSchemaDto[].class);
	}

	/**
	 * This function gets all the data about the stations via an api from famas
	 *
	 * @return MetadataDto-array with all the stations
	 * @throws IOException used in the code to throw a failure in input and output operations
	 */
	public MetadataDto[] getStationsData() throws IOException {
		// TODO: Comment out code and remove test json files if api is available
		//HttpResponse response = client.execute(new HttpGet(stationsDataUrl));
		//HttpEntity entity = response.getEntity();
		//String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		String responseString = readJsonFile("jsonfiles/stationsData.json");
		return objectMapper.readValue(responseString, MetadataDto[].class);
	}

	/**
	 * This function gets all the traffic data about the stations via an api from famas
	 *
	 * @return AggregatedDataDto-array with all traffic of the stations
	 * @throws IOException used in the code to throw a failure in input and output operations
	 */
	public AggregatedDataDto[] getAggregatedDataOnStations(String stationId, String startPeriod, String endPeriod) throws IOException {
		// TODO: Comment out code and remove test json files if api is available
		//JSONObject payload = new JSONObject();
		//JSONArray stationIdArray = new JSONArray();
		//stationIdArray.add(stationId);
		//payload.put("IdPostazioni", stationIdArray);
		//payload.put("InizioPeriodo", startPeriod);
		//payload.put("FinePeriodo", endPeriod);
		//StringEntity stringEntity = new StringEntity(String.valueOf(payload),
		//	ContentType.APPLICATION_JSON);
		//HttpPost request = new HttpPost(aggregatedDataOnStationsUrl);
		//request.setEntity(stringEntity);
		//HttpEntity entity = client.execute(request).getEntity();
		//String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		String responseString = readJsonFile("jsonfiles/aggregatedDataOnStations.json");
		return objectMapper.readValue(responseString, AggregatedDataDto[].class);
	}

	/**
	 * This function gets all the bluetooth addressess about the devices passed the stations via an api from famas
	 *
	 * @return PassagesDataDto-array with all bluetooth devices passed the stations
	 * @throws IOException used in the code to throw a failure in input and output operations
	 */
	public PassagesDataDto[] getPassagesDataOnStations(String stationId, String startPeriod, String endPeriod) throws IOException {
		// TODO: Comment out code and remove test json files if api is available
		//JSONObject payload = new JSONObject();
		//JSONArray stationIdArray = new JSONArray();
		//stationIdArray.add(stationId);
		//payload.put("IdPostazioni", stationIdArray);
		//payload.put("InizioPeriodo", startPeriod);
		//payload.put("FinePeriodo", endPeriod);
		//StringEntity stringEntity = new StringEntity(String.valueOf(payload),
		//	ContentType.APPLICATION_JSON);
		//HttpPost request = new HttpPost(passagesDataOnStationsUrl);
		//request.setEntity(stringEntity);
		//HttpEntity entity = client.execute(request).getEntity();
		//String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		String responseString = readJsonFile("jsonfiles/passagesDataOnStations.json");
		return objectMapper.readValue(responseString, PassagesDataDto[].class);
	}

	/**
	 * TODO: Remove helper class after calling live api
	 *
	 * Read json test files and return them
	 *
	 * @param url is a string where the file location is stored
	 * @return a string containing the data of the json file
	 */
	public String readJsonFile(String url) {
		JSONParser jsonParser = new JSONParser();
		try (FileReader reader = new FileReader(url))
		{
			return String.valueOf(jsonParser.parse(reader));

		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
