package com.sv2x.googlemap3;

import org.json.JSONArray;

/**
 * Created by netlab on 9/21/16.
 */

//new commit
public class Parser
{
    private ParsingFinishedListener callback;

    public Parser(ParsingFinishedListener c) {
        this.callback = c;
    }

    //some code

    public void parse(JSONArray stuffToParse, JSONArray array_of_instruction) {
        //code
        callback.onTextParsed(stuffToParse,array_of_instruction);
    }

    public interface ParsingFinishedListener {
        public void onTextParsed(JSONArray textToVizualize,JSONArray array_of_instruction);
    }
}