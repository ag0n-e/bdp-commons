package it.fos.noibz.skyalps.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.fos.noibz.skyalps.dto.json.schedule.AeroCRSFlight;
import it.fos.noibz.skyalps.dto.json.schedule.AeroCRSGetScheduleSuccessResponse;
import it.fos.noibz.skyalps.rest.AeroCRSRest;

//This class is responsible to schedule retrieve and push operations for Stations.
//It has been structured in the same way the meteorology eurac data collector does. 
@Lazy
@Service
public class SyncScheduler {
	/**
	 * Cronjob configuration can be found under
	 * src/main/resources/META-INF/spring/applicationContext.xml XXX Do not forget
	 * to configure it!
	 */
	private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

	@Value("${odh_client.STATION_ID_PREFIX}")
	private String STATION_ID_PREFIX;

	@Value("${odh_client.LA}")
	private Double LA;

	@Value("${odh_client.LON}")
	private Double LON;

	@Value("${sched_days_before}")
	private int days_before;

	@Value("${sched_days_after}")
	private int days_after;

	@Value("${ssim_enabled}")
	private boolean ssimEnabled;

	@Autowired
	AeroCRSConst aeroconst;

	@Autowired
	RestTemplate template;

	@Lazy
	@Autowired
	private AeroCRSRest acrsclient;

	@Lazy
	@Autowired
	private ODHClient odhclient;

	@Autowired
	Environment env;

	/**
	 * Scheduled job A: sync stations and data types
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */

	@SuppressWarnings("static-access")
	@Scheduled(cron = "${scheduler.job_stations}")
	public void syncJobStations() throws IOException, ParseException {

		//////////////////////////
		// Sync stations
		/////////////////////////
		// Get current date
		// Subtracting certain amount of days defined in the application.properties
		// Adding certain amount of days defined in the application.properties
		// Synch stations in between the date

		StationList odhStationlist = new StationList();

		LOG.info("Cron job started: Sync Stations with type {} and data types", odhclient.getIntegreenTypology());
		for (int i = 0 - days_before; i < days_after; i++) {

			Calendar from = Calendar.getInstance();
			from.add(Calendar.DATE, i);
			Date fltsFROMPeriod = from.getTime();
			Calendar to = Calendar.getInstance();
			to.add(Calendar.DATE, i + 1);
			Date fltsTOPeriod = to.getTime();

			AeroCRSGetScheduleSuccessResponse aero = acrsclient.getSchedule(template, fltsFROMPeriod, fltsTOPeriod,
					aeroconst.getIatacode(), aeroconst.getCompanycode(), false, ssimEnabled);

			for (AeroCRSFlight dto : aero.getAerocrs().getFlight()) {

				StationDto stationDto = new StationDto();
				stationDto.setId(dto.getFltnumber() + "_" + dto.getDate());
				stationDto.setName(dto.getFromdestination() + "-" + dto.getTodestination());
				stationDto.setOrigin(odhclient.getProvenanceOrigin());
				stationDto.setLatitude(LA);
				stationDto.setLongitude(LON);

				Map<String, Object> metaData = new HashMap<>();
				metaData.put(aeroconst.getAirlineId(), dto.getAirlineid());
				metaData.put(aeroconst.getAirlinename(), dto.getAirlinename());
				metaData.put(aeroconst.getAccode(), dto.getAccode());
				metaData.put(aeroconst.getFltnumber(), dto.getFltnumber());
				metaData.put(aeroconst.getFltstoperiod(), dto.getFltstoperiod());
				metaData.put(aeroconst.getFltsfromperiod(), dto.getFltsfromperiod());
				metaData.put(aeroconst.getSta(), dto.getSta());
				metaData.put(aeroconst.getStd(), dto.getStd());
				metaData.put(aeroconst.getWeekdaymon(), dto.getWeekdaymon());
				metaData.put(aeroconst.getWeekdaytue(), dto.getWeekdaytue());
				metaData.put(aeroconst.getWeekdaywed(), dto.getWeekdaywed());
				metaData.put(aeroconst.getWeekdaythu(), dto.getWeekdaythu());
				metaData.put(aeroconst.getWeekdayfri(), dto.getWeekdayfri());
				metaData.put(aeroconst.getWeekdaysat(), dto.getWeekdaysat());
				metaData.put(aeroconst.getWeekdaysun(), dto.getWeekdaysun());
				metaData.put(aeroconst.getFromdestination(), dto.getFromdestination());
				metaData.put(aeroconst.getTodestination(), dto.getTodestination());
				// unix timestamp
				metaData.put("departure_timestamp", dto.getDepartureTimestamp());
				metaData.put("arrival_timestamp", dto.getArrivalTimestamp());
				metaData.put("ssim", dto.getSsimMessage());
				stationDto.setMetaData(metaData);

				odhStationlist.add(stationDto);

			}
		}

		LOG.info("Trying to sync the stations...");
		odhclient.syncStations(odhclient.getIntegreenTypology(), odhStationlist);
		LOG.info("Syncing stations done.");

	}

}