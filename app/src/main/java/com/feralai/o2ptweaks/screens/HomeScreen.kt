package com.feralai.o2ptweaks.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.feralai.o2ptweaks.utils.SystemUtils
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme

private var LABEL_WIDTH = 240.dp
private var COL_MARGIN = 6.dp
private var DATA_ROW_PADDING = PaddingValues(0.dp, 0.dp, 0.dp, 1.dp)

@Composable
fun HomeScreen (
    modifier: Modifier = Modifier,
    context: Context,
    sharedPrefs: SharedPreferences? = null,
) {
    Column(
        modifier = modifier.verticalScroll(
            state = rememberScrollState(),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ).fillMaxHeight().fillMaxWidth().padding(PaddingValues(16.dp, 4.dp)),
    ) {
        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Device Model:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropDeviceModel())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Device Platform:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropFotaPlatform())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Firmware Version:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropFirmwareVersion())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Firmware Build Date:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropBuildDate())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Firmware Build Name:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropBuildDisplayId())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Firmware Build ID:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropBuildId())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Active Boot Slot:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getPropSlot())
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Current Battery Capacity:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getSystemBatteryChargeCounter(context).toString() + " mAh")
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Max Battery Capacity:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getSystemBatteryCapacity(context).toString() + " mAh")
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Designed Battery Capacity:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getSystemBatteryCapacityFull(context).toString() + " mAh")
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Battery Health:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getSystemBatteryHealthPercent(context).toString() + "% (${SystemUtils.getSystemBatteryHealthLabel(context)})")
        }

        Row(
            modifier = modifier.fillMaxWidth().padding(DATA_ROW_PADDING)
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH),
                fontWeight = FontWeight.Bold,
                text = "Kernel Version:",
                textAlign = TextAlign.Right,
            )
            Spacer(modifier = modifier.padding(COL_MARGIN))
            Text(SystemUtils.getKernelVersion(context))
        }


    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=960dp,height=540dp,dpi=369,orientation=landscape"
)
@Composable
fun HomeScreenPreview() {
    val context = LocalContext.current

    o2ptweaksTheme {
        HomeScreen(context = context)
    }
}