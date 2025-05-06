package com.feralai.o2ptweaks.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp


private var DATA_ROW_PADDING = PaddingValues(0.dp, 0.dp, 0.dp, 1.dp)

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    context: Context,
) {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val version = pInfo.versionName
    val linkColor = Color(3, 169, 244, 255)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "O2P Tweaks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/FeralAI/o2ptweaks.app/blob/main/docs/USERGUIDE.md",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("User Guide")
                    }
                }
            )
        }

        Spacer(modifier = modifier.padding(PaddingValues(0.dp, 16.dp)))

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    append("Made with ❤ by ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/FeralAI",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("@FeralAI")
                    }
                }
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "Open source under the GPLv2 license")
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/FeralAI/o2ptweaks.app",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("https://github.com/FeralAI/o2ptweaks.app")
                    }
                }
            )
        }

        Spacer(modifier = modifier.padding(PaddingValues(0.dp, 16.dp)))

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "Special thanks to:")
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/kokoko3k",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("@kokoko3k")
                    }
                    append(" for the jdsp4rp5.app, which inspired this app")
                }
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/james34602",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("@james34602")
                    }
                    append(" for the JamesDSP library")
                }
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(DATA_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/timschneeb",
                            TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) {
                        append("@timschneeb")
                    }
                    append(" for the JamesDSP manager app")
                }
            )
        }
    }
}
