package com.laporeon.picpay.dtos.response;

public record AuthorizationResponse(
        String status,
        Data data
) {
        public record Data(boolean authorization) {}

        public boolean isAuthorized() {
                return "success".equalsIgnoreCase(status)
                        && data != null
                        && data.authorization();
        }
}
