package org.noria.cdafhirlib.model;

import lombok.Data;

import java.util.List;

@Data
public class FHIRtoCDACodes {
    private List<CodeToCodeMappingElement> fhirCdaMappings;
}
