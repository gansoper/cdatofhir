package org.noria.cdafhirlib.model;

import lombok.Data;

import java.util.List;

@Data
public class CDAtoFHIRCodeElement {
    private String type;
    private List<CodesMapping> mapping;
}
