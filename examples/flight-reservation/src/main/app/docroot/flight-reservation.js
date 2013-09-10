/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
function onload() {
	dojo.byId("error").style.display = "none";
	dojo.byId("searchResults").style.display = "none";
}

function makeSearch(origin, destination) {
	var request="";
		
	if (origin == "BUE" && destination == "MOW") {
		var request = {"flights": [{"flightNumber":915},{"flightNumber":1022},{"flightNumber":730}]};
		mule.rpc("/searchFlights", JSON.stringify(request), processResponse);
	} else if (origin == "BUE" && destination == "HKG") {
		var request = {"flights":[{"flightNumber":822},{"flightNumber":1133}]};
		mule.rpc("/searchFlights", JSON.stringify(request), processResponse);
	} else {
		var request={"Invalid Request":[]};
		mule.rpc("/searchFlights", JSON.stringify(request), processResponse);
	}
}

function processResponse(message) {
	flights = JSON.parse("[" + message.data + "]")[0];
	if(flights.errors == "") {
		dojo.byId("error").style.display = "none";
		dojo.byId("searchResults").style.display = "block";

		var results = "<table class='results'>";
		results += "<th>Flight Number</th><th>Seat assignment</th><th>Price</th>"
		for(var i = 0; i < flights.flights.length;i++) {
			results +="<tr><td>" + flights.flights[i].flightNumber + "</td><td>" + flights.flights[i].seatInfo + "</td><td>$" + flights.flights[i].ticketPrice + "</td></tr>";
		}
		results += "<tr><td colspan='3'><div id='totalPrice'>Total price is $" + flights.totalPrice + "</div></td><tr>"
		results += "</table>";

		dojo.byId("searchResults").innerHTML = results;
	} else {
		dojo.byId("error").style.display = "block";
		dojo.byId("searchResults").style.display = "none";
		dojo.byId("errorMessage").innerHTML = flights.errors;
	}
}

