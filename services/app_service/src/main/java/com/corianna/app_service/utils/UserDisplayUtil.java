package com.corianna.app_service.utils;

public class UserDisplayUtil {
    public static String formatUserDisplayName(String firstName, String lastName, String email) {
        // Validate that email is provided (required field)
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // Clean up the inputs - trim whitespace and treat empty strings as null
        String cleanFirstName = (firstName != null && !firstName.trim().isEmpty()) ? firstName.trim() : null;
        String cleanLastName = (lastName != null && !lastName.trim().isEmpty()) ? lastName.trim() : null;
        String cleanEmail = email.trim();

        // Build the display name based on available information
        StringBuilder displayName = new StringBuilder();

        // Add first name if available
        if (cleanFirstName != null) {
            displayName.append(cleanFirstName);
        }

        // Add last name if available
        if (cleanLastName != null) {
            if (displayName.length() > 0) {
                displayName.append(" "); // Add space between first and last name
            }
            displayName.append(cleanLastName);
        }

        // Add email in parentheses
        if (displayName.length() > 0) {
            displayName.append("(").append(cleanEmail).append(")");
        } else {
            // If no first name or last name, return email only
            return cleanEmail;
        }

        return displayName.toString();
    }

}
