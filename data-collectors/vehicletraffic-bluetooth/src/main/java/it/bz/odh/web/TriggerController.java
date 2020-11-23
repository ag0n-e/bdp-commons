package it.bz.odh.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.odh.service.OddsPusher;
import it.bz.odh.util.BluetoothMappingUtil;

@RequestMapping("/trigger")
@Controller
@EnableWebMvc
public class TriggerController {
	
	private Logger logger = LogManager.getLogger(TriggerController.class);

	@Autowired
	private OddsPusher pusher;

	@Autowired
	private BluetoothMappingUtil metaUtil;

    @Value("${origin}")
	private String origin;

	/**
	 * Endpoint call for google notification service
	 * https://developers.google.com/drive/api/v3/push. As soon as the call is
	 * triggered the cache gets invalidated and station synchronize with the
	 * existing BluetoothStations in ODH.
	 *
	 * @param gDto request body of push notification by google as defined in
	 *             https://developers.google.com/drive/api/v3/push
	 */
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody void post(@RequestBody(required = false) GooglePushDto gDto,
			@RequestHeader(required = true, value = "x-goog-changed") String whatChanged) {
		logger.debug("Sync triggered");
		if ("content".equals(whatChanged)) {
			logger.debug("Call is content related");
			metaUtil.setCachedData(null);
			logger.debug("Fetch Stations from odh");
			List<StationDto> odhStations = pusher.fetchStations(pusher.getIntegreenTypology(), origin);
			List<String> stationIds = odhStations.stream().map(StationDto::getId).collect(Collectors.toList());
			logger.debug("Found "+stationIds.size()+" Bluetoothboxes");
			StationList stations = new StationList();
			for (Map<String, String> entry : metaUtil.getValidEntries()) {
				String stationId = entry.get("id");
				logger.debug("Extract station with id: "+stationId);
				if (!stationIds.contains(stationId))
					continue;
				logger.debug("Map station with id "+stationId);
				Double[] coordinatesByIdentifier = metaUtil.getCoordinatesByIdentifier(stationId);
				Map<String, Object> metaDataByIdentifier = metaUtil.getMetaDataByIdentifier(stationId);
				logger.debug("Start merging translations");
				metaUtil.mergeTranslations(metaDataByIdentifier);
				StationDto dto = new StationDto();
				dto.setName(stationId);
				dto.setId(stationId);
				dto.setLongitude(coordinatesByIdentifier[0]);
				dto.setLatitude(coordinatesByIdentifier[1]);
				if (!metaDataByIdentifier.isEmpty())
					dto.setMetaData(metaDataByIdentifier);
				stations.add(dto);
				logger.debug("Dto created and added");
			}
			if (!stations.isEmpty()) {
				logger.debug("Push data to odh");
				pusher.syncStations(stations);
				logger.debug("Finished pushing to odh");
			}
		}
	}

}