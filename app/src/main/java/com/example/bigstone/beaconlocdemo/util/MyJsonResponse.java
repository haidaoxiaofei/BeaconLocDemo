package com.example.bigstone.beaconlocdemo.util;

import org.json.JSONObject;

/**
 * MyJsonResponse class
 * */
public class MyJsonResponse {

	public static enum TYPES_ENUM {
		SUCCESS, WARNING, ERROR
	};

	private String content = "";

	private TYPES_ENUM type = TYPES_ENUM.ERROR;

	public MyJsonResponse() {
		this.setContent("Oops something went wrong! Possible reasons is internet connectivity or server is temporarily down. If the problem persists please contact the administrator.");
		this.setType(TYPES_ENUM.ERROR);
	}

	public MyJsonResponse(String content, TYPES_ENUM type) {
		this.setContent(content);
		this.setType(type);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public TYPES_ENUM getType() {
		return type;
	}

	public void setType(TYPES_ENUM type) {
		this.type = type;
	}

	public void setType(String type_str) {
		if (type_str.equalsIgnoreCase("Error"))
			this.type = TYPES_ENUM.ERROR;
		else if (type_str.equalsIgnoreCase("Success"))
			this.type = TYPES_ENUM.SUCCESS;
		else if (type_str.equalsIgnoreCase("Warning"))
			this.type = TYPES_ENUM.WARNING;
		else
			this.type = TYPES_ENUM.ERROR;

	}

	/****************************************************************************************************************/

	/**
	 * Creates a MyJsonResponse with status and format depending on the
	 * following responses: Success, Failed, Warning
	 * 
	 * @param json_resp
	 *            the string returned from server in JSON format
	 * @return a MyJsonResponse object, containing a status and content
	 * */
	public static MyJsonResponse getResponse(JSONObject json_resp) {

		MyJsonResponse obj = new MyJsonResponse();

		try {

			// Set attributes of MyJsonResponse object based on the JSON
			// response
			obj.setType(json_resp.getString("status"));
			obj.setContent(json_resp.getString("msg"));
			return obj;

		} catch (Exception e) {
			obj.setType(TYPES_ENUM.ERROR);
			obj.setContent("Data are not valid.");
			return obj;
		}

	}
	
}
