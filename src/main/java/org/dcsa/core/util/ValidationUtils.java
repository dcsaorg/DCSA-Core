package org.dcsa.core.util;

public class ValidationUtils {

    /**
     * Validate if a Vessel IMO Number is correct according to "Structure" defined here: https://en.wikipedia.org/wiki/IMO_number
     *
     * @param vesselIMONumber the number (as a string) to verify
     *
     */
    public static void validateVesselIMONumber(String vesselIMONumber) throws IllegalArgumentException {
        if (vesselIMONumber != null && vesselIMONumber.length() == 7) {
            int sum = 0;
            for (int i = 0; i < 6; i++) {
                char c = vesselIMONumber.charAt(i);
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException("Invalid Vessel IMO Number. It must consist entirely of digits");
                }
                sum += (7 - i) * Character.getNumericValue(c);
            }
            String s = String.valueOf(sum);
            if (vesselIMONumber.charAt(vesselIMONumber.length() - 1) != s.charAt(s.length() - 1)) {
                throw new IllegalArgumentException("Invalid Vessel IMO Number. IMO number does not pass checksum - expected value: " +  s.charAt(s.length() - 1) + " but found: " + vesselIMONumber.charAt(vesselIMONumber.length() - 1));
            }
        } else {
            throw new IllegalArgumentException("Invalid Vessel IMO Number. Must match 7-digits");
        }
    }
}
