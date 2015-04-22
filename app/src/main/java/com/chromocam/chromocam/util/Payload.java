package com.chromocam.chromocam.util;

import org.json.JSONObject;

/**
 * Created by Aleks on 4/19/2015.
 */
public class Payload{
    private JSONObject post;
    private String URL;
    private Purpose purpose;
    private boolean result; //true if good result, false if bad result

    public Payload(JSONObject thing, String URL, Purpose purpose){
        this.post = thing;
        this.URL = URL;
        this.purpose = purpose;
    }

    public String getURL(){
        return this.URL;
    }

    public JSONObject getPost(){
        return this.post;
    }

    public boolean getResult(){
        return this.result;
    }

    public void setResult(boolean result){
        this.result = result;
    }
}
