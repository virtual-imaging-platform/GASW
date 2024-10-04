package fr.insalyon.creatis.gasw;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GaswUtil tests")
public class GaswUtilTest {
    @Test
    @DisplayName("Uri detection")
    public void uriDetection() {
        assertTrue(GaswUtil.isUri("girder:///control_3DT1.nii"));
        assertTrue(GaswUtil.isUri("ssh://example.com/control_3DT1.nii"));
        assertTrue(GaswUtil.isUri("girder:/control_3DT1.nii"));

        assertFalse(GaswUtil.isUri("girder:control_3DT1.nii"));
        assertFalse(GaswUtil.isUri("girder:////control_3DT1.nii"));
    }
}

