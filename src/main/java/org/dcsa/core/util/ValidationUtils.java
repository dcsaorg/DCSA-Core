package org.dcsa.core.util;

import java.security.InvalidParameterException;

public class ValidationUtils {

    /**
     * Validate if a Vessel IMO Number is correct according to "Structure" defined here: https://en.wikipedia.org/wiki/IMO_number
     *
     * @param vesselIMONumber the number (as a string) to verify
     *
     * @return true if the vessel IMO Number is valid otherwise false
     */
    public static boolean validateVesselIMONumber(String vesselIMONumber) throws InvalidParameterException {
        if (vesselIMONumber != null && vesselIMONumber.length() == 7) {
            int sum = 0;
            for (int i = 0; i < 6; i++) {
                sum += (7 - i) * (int) vesselIMONumber.charAt(i);
            }
            String s = String.valueOf(sum);
            if (vesselIMONumber.charAt(vesselIMONumber.length() - 1) == s.charAt(s.length() - 1)) {
                return true;
            } else {
                throw new InvalidParameterException("Invalid Vessel IMO Number. IMO number does not pass checksum - expected value: " + vesselIMONumber.charAt(vesselIMONumber.length() - 1) + " but found: " + s.charAt(s.length() - 1));
            }
        } else {
            throw new InvalidParameterException("Invalid Vessel IMO Number. Must match 7-digits");
        }
    }
}
