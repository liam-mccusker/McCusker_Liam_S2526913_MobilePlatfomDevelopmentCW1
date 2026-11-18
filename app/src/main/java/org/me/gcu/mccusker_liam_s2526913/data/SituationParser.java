package org.me.gcu.mccusker_liam_s2526913.data;

import android.util.Log;

import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;

public class SituationParser {

    public ArrayList<Situation> parse(String xml, String category) {
        ArrayList<Situation> situations = new ArrayList<>();
        Situation current = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();

                    if (tag.equals("situation")) {
                        current = new Situation();
                        current.setCategory(category);
                        current.setId(parser.getAttributeValue(null, "id"));
                    } else if (current != null && tag.equals("situationRecord")) {
                        current.setRecordType(parser.getAttributeValue(null, "xsi:type"));
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("situation") && current != null) {
                        situations.add(current);
                        current = null;
                    }
                }

                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("SituationParser", "Parsing failed", e);
        }

        return situations;
    }
}