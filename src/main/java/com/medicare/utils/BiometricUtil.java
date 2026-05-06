package com.medicare.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Invokes Windows Hello (fingerprint / face / PIN) via PowerShell + WinRT UserConsentVerifier.
 * The OS native dialog appears on top while verify() blocks in a background thread.
 */
public final class BiometricUtil {

    public enum Result { VERIFIED, FAILED, UNAVAILABLE, ERROR }

    // Checks whether Windows Hello biometrics are available and configured for the current Windows user
    private static final String CHECK_SCRIPT =
        "Add-Type -AssemblyName System.Runtime.WindowsRuntime; " +
        "[void][Windows.Security.Credentials.UI.UserConsentVerifier," +
            "Windows.Security.Credentials.UI,ContentType=WindowsRuntime]; " +
        "$m = [System.WindowsRuntimeSystemExtensions].GetMethods() | " +
            "Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and " +
            "$_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | " +
            "Select-Object -First 1; " +
        "$op = [Windows.Security.Credentials.UI.UserConsentVerifier]::CheckAvailabilityAsync(); " +
        "$t  = $m.MakeGenericMethod(" +
            "[Windows.Security.Credentials.UI.UserConsentVerifierAvailability]" +
            ").Invoke($null, @($op)); " +
        "$t.Wait(); Write-Output $t.Result";

    // Triggers the Windows Hello dialog; PROMPT is replaced by the caller
    private static final String VERIFY_SCRIPT_TEMPLATE =
        "Add-Type -AssemblyName System.Runtime.WindowsRuntime; " +
        "[void][Windows.Security.Credentials.UI.UserConsentVerifier," +
            "Windows.Security.Credentials.UI,ContentType=WindowsRuntime]; " +
        "$m = [System.WindowsRuntimeSystemExtensions].GetMethods() | " +
            "Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and " +
            "$_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | " +
            "Select-Object -First 1; " +
        "$op = [Windows.Security.Credentials.UI.UserConsentVerifier]::RequestVerificationAsync('%s'); " +
        "$t  = $m.MakeGenericMethod(" +
            "[Windows.Security.Credentials.UI.UserConsentVerificationResult]" +
            ").Invoke($null, @($op)); " +
        "$t.Wait(); Write-Output $t.Result";

    private BiometricUtil() {}

    /** Returns true if Windows Hello is available and configured for the current Windows session. */
    public static boolean isAvailable() {
        try {
            String out = runPowerShell(CHECK_SCRIPT).trim();
            return "Available".equalsIgnoreCase(out);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Triggers the native Windows Hello prompt (fingerprint / face / PIN).
     * Blocks until the user responds or cancels. Call from a background thread only.
     *
     * @param message text shown inside the Windows Hello dialog
     */
    public static Result verify(String message) {
        try {
            String safe   = message == null ? "Medicare" : message.replace("'", "''");
            String script = String.format(VERIFY_SCRIPT_TEMPLATE, safe);
            String out    = runPowerShell(script).trim();
            return switch (out) {
                case "Verified"             -> Result.VERIFIED;
                case "DeviceNotPresent",
                     "NotConfiguredForUser",
                     "DisabledByPolicy"     -> Result.UNAVAILABLE;
                default                     -> Result.FAILED;   // Canceled, RetriesExhausted, …
            };
        } catch (Exception e) {
            return Result.ERROR;
        }
    }

    private static String runPowerShell(String script) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe", "-NonInteractive", "-NoProfile", "-Command", script
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        proc.waitFor();
        return sb.toString();
    }
}
