// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.spreadsheets.services;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Channel;

@Lazy
@Service
public class SpreadsheetWatcher extends GoogleAuthenticator {
	private static final String NOTIFICATION_TYPE = "web_hook";

	private Drive service;

	@Value("${spreadsheetId}")
	private String spreadsheetId;

	@Value("${spreadsheet_notificationUrl}")
	private String notificationUrl;

	private Logger logger = LoggerFactory.getLogger(SpreadsheetWatcher.class);

	public void registerWatch() {
		try {
			logger.debug("Start creating notification channel");
			Channel channel = new Channel();
			channel.setId(UUID.randomUUID().toString());
			channel.setType(NOTIFICATION_TYPE);
			channel.setAddress(notificationUrl);
			service.files().watch(spreadsheetId, channel).execute();
			logger.debug("Created notification channel for spreadsheet"+spreadsheetId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initGoogleClient(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, Credential credential)
			throws IOException {
		service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName("google spreadsheet collector").build();
	}
}
