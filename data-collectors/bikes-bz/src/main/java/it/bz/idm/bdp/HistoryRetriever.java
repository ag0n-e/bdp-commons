// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationDto;

@Component
@PropertySource("classpath:/META-INF/spring/application.properties")
public class HistoryRetriever {

	private static final int TIME_INTERVAL = 1000*60*60*6;

	@Autowired
	public DataParser parser;

	@Autowired
	private BikeCountPusher bdpClient;

	@Autowired
	public Environment environment;

	private static final Logger LOG = LoggerFactory.getLogger(HistoryRetriever.class);

	/*retrieve all history data from a given point in time and push it to the bdp */
	public void getHistory(LocalDateTime newestDateMidnight) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String startDate = environment.getProperty("history.startDate");
		LocalDateTime manualDate = LocalDate.parse(startDate, format).atStartOfDay();
		if (newestDateMidnight == null || manualDate.isAfter(newestDateMidnight))
			newestDateMidnight = manualDate;
		try {
			XMLGregorianCalendar from = null;
			XMLGregorianCalendar to = null;
			GregorianCalendar cal = new GregorianCalendar();
			Duration duration = DatatypeFactory.newInstance().newDuration(TIME_INTERVAL);
			while (newestDateMidnight.isBefore(LocalDateTime.now())) {
				cal.setTimeInMillis(newestDateMidnight.toInstant(ZoneOffset.UTC).toEpochMilli());
				from = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
				to = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
				to.add(duration);
				LOG.debug("About To request Data");
				Long now = new Date().getTime();
				DataMapDto<RecordDtoImpl> dataMapDto = parser.retrieveHistoricData(from, to);
				LOG.debug("3rd party took" + (new Date().getTime() - now) / 1000 + " s");
				bdpClient.pushData(dataMapDto);
				LOG.debug("Data sent");
				newestDateMidnight = LocalDateTime
						.ofInstant(Instant.ofEpochMilli(to.toGregorianCalendar().getTimeInMillis()), ZoneOffset.UTC);
			}
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void getLatestHistory() {
		Date newestDate = fetchNewestExistingDate();
		LocalDateTime newestDateMidnight = LocalDateTime.of(
				LocalDateTime.ofInstant(Instant.ofEpochMilli(newestDate.getTime()), ZoneOffset.UTC).toLocalDate(),
				LocalTime.MIDNIGHT);
		getHistory(newestDateMidnight);
	}

	public Date fetchNewestExistingDate() {
		LOG.debug("Start fetching newest records for all stations");
		List<Date> dateList = new ArrayList<>();
		List<StationDto> fetchStations = bdpClient.fetchStations(BikeCountPusher.STATIONTYPE_IDENTIFIER,
				DataParser.DATA_ORIGIN);
		LOG.debug("Number of fetched stations" + fetchStations.size());
		for (StationDto dto : fetchStations) {
			Date hui = new Date();
			Date dateOfLastRecord = (Date) bdpClient.getDateOfLastRecord(dto.getId(), null, null);
			if (dateOfLastRecord != null)
				dateList.add(dateOfLastRecord);
			LOG.debug("Querry took" + (new Date().getTime() - hui.getTime()));
		}
		Collections.sort(dateList);
		return dateList.isEmpty() ? null : dateList.get(dateList.size() - 1);
	}

}
