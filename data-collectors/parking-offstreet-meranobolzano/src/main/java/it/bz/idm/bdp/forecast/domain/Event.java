// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.forecast.domain;


public class Event implements ObservationMetaInfo{
	
	private String description = "no event registered";
	
	public void setId(int id) {
		this.id = id;
	}

	private int id= -1;
	
	public Event(String descr, int id){
		this.description = descr;
		this.id = id;
	}
	
	public Event(){
		this.description = "";
		this.id = -1;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getId() {
		return id;
	}
}
