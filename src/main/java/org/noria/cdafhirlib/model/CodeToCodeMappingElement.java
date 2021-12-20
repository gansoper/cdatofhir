package org.noria.cdafhirlib.model;

import lombok.Data;

import java.util.List;

@Data
public class CodeToCodeMappingElement {
    private String type;
    private List<CodesMapping> mapping;
}
