package com.moko.support.entity;

import java.util.List;

public class FilterCondition {
    /**
     * rule_switch : 1
     * rssi : -127
     * name : {"flag":1,"rule":"MK107"}
     * mac : {"flag":0,"rule":""}
     * major : {"flag":2,"min":1234,"max":5678}
     * minor : {"flag":0,"min":1234,"max":5678}
     * uuid : {"flag":1,"rule":"0102030405060708090A0B0C0D0E0F"}
     * raw : {"flag":1,"rule":[]}
     */

    public int rule_switch;
    public int rssi;
    public NameBean name;
    public MacBean mac;
    public MajorBean major;
    public MinorBean minor;
    public UUIDBean uuid;
    public RawBean raw;


    public static class NameBean {
        /**
         * flag : 1
         * rule : MK107
         */

        public int flag;
        public String rule;
    }

    public static class MacBean {
        /**
         * flag : 0
         * rule :
         */

        public int flag;
        public String rule;
    }

    public static class MajorBean {
        /**
         * flag : 2
         * min : 1234
         * max : 5678
         */

        public int flag;
        public int min;
        public int max;
    }

    public static class MinorBean {
        /**
         * flag : 0
         * min : 1234
         * max : 5678
         */

        public int flag;
        public int min;
        public int max;
    }

    public static class UUIDBean {
        /**
         * flag : 1
         * rule : 0102030405060708090A0B0C0D0E0F
         */

        public int flag;
        public String rule;

    }

    public static class RawBean {
        public int flag;
        public List<RawDataBean> rule;

    }

    public static class RawDataBean {
        public String type;
        public int start;
        public int end;
        public String data;
    }
}
