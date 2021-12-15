package org.noria.cdafhirlib.model;

import lombok.Data;

@Data
public class CodesMapping {
       private String sourceCode;
       private CDAFHIRCommonCode targetCode;
}
