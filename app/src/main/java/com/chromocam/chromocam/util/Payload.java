package com.chromocam.chromocam.util;

import com.chromocam.chromocam.EventContent;

import org.json.JSONObject;

import java.util.ArrayList;

public class Payload{
    private JSONObject response;
    private ArrayList<EventContent> content = new ArrayList<EventContent>();
    private String URL;
    private Purpose purpose;
    private boolean result; //true if good result, false if bad result

    public Payload(String URL, Purpose purpose){
        this.URL = URL;
        this.purpose = purpose;
    }

    public String getURL(){
        return this.URL;
    }

    public JSONObject getResponse(){
        return this.response;
    }

    public void setResponse(JSONObject response){ this.response = response; }

    public boolean getResult(){
        return this.result;
    }

    public Purpose getPurpose() { return this.purpose; }

    public void setResult(boolean result){
        this.result = result;
    }

    public void setContent(ArrayList<EventContent> list) { this.content = list; }

    public ArrayList<EventContent> getContent() { return this.content; }
}
