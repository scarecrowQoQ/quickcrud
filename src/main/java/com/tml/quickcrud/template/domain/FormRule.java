package com.tml.quickcrud.template.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data

public class FormRule {

    private HashMap<String, List<Rule>> rules;

    @Data
    public static class Rule{
        private Boolean required;

        private String message;

        private Trigger trigger;

    }

    public enum Trigger {
        blur,
        change
    }

    public FormRule() {
        this.rules = new HashMap<>();
    }

}

