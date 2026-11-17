package com.laporeon.picpay.enums;

public enum WalletType {
    COMMON("Common users", "CPF", 11),
    SHOPKEEPER("Shopkeeper", "CNPJ", 14);

    private final String label;
    private final String documentName;
    private final Integer documentLength;

    WalletType(String label, String documentName, Integer documentLength) {
        this.label = label;
        this.documentName = documentName;
        this.documentLength = documentLength;
    }

    public String getLabel() {
        return label;
    }

    public String getDocumentName() {
        return documentName;
    }

    public int getDocumentLength() {
        return documentLength;
    }
}
