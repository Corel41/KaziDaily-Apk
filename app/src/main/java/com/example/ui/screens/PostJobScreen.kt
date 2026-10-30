package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.JobCategory
import com.example.model.LocationCoordinate
import com.example.model.NairobiLocations
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LocationPickerMap
import com.example.ui.theme.LocalGlassTokens
import com.example.util.CloudinaryService
import com.example.viewmodel.KaziViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    viewModel: KaziViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(JobCategory.CLEANING) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedLocation by remember { mutableStateOf(NairobiLocations.PRESET_LOCATIONS[1]) }
    var locationExpanded by remember { mutableStateOf(false) }
    var showLocationPickerMap by remember { mutableStateOf(false) }

    var durationHoursText by remember { mutableStateOf("3") }
    var payKshText by remember { mutableStateOf("2500") }
    var description by remember { mutableStateOf("") }

    // Job site photos uploaded to Cloudinary
    val uploadedPhotoUrls = remember { mutableStateListOf<String>() }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingPhoto = true
                viewModel.showToast("Uploading site photo to Cloudinary CDN...")
                val res = CloudinaryService.uploadImage(context, uri, folder = "kazidaily_jobs")
                isUploadingPhoto = false
                res.onSuccess { url ->
                    if (uploadedPhotoUrls.size < 3) {
                        uploadedPhotoUrls.add(url)
                        viewModel.showToast("Site photo attached! (${uploadedPhotoUrls.size}/3)")
                    }
                }.onFailure { err ->
                    viewModel.showToast("Upload failed: ${err.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    val payKsh = payKshText.toIntOrNull() ?: 0
    val platformFeeKsh = (payKsh * 10) / 100
    val netWorkerPayout = payKsh - platformFeeKsh

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Post a Daily Gig",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "Hire reliable, verified workers in Nairobi with escrow protection",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // Job Title
                Text(
                    text = "Job Title",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Apartment Deep Cleaning", fontSize = 13.sp, color = tokens.textMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surfaceGlass,
                        unfocusedContainerColor = tokens.surfaceGlass,
                        focusedBorderColor = tokens.mintAccent,
                        unfocusedBorderColor = tokens.borderGlass,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.iconName} ${selectedCategory.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.surfaceGlass,
                            unfocusedContainerColor = tokens.surfaceGlass,
                            focusedBorderColor = tokens.mintAccent,
                            unfocusedBorderColor = tokens.borderGlass,
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        containerColor = tokens.surfaceElevated
                    ) {
                        JobCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.iconName} ${cat.displayName}", color = tokens.textPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Picker (Maps Preset & Pin Drop)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Job Location (Nairobi Region)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textSecondary
                    )
                    TextButton(
                        onClick = { showLocationPickerMap = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("open_map_location_picker_button")
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = tokens.mintAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pin on Map", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.mintAccent)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = !locationExpanded }
                ) {
                    OutlinedTextField(
                        value = "📍 ${selectedLocation.name} (${selectedLocation.description})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.surfaceGlass,
                            unfocusedContainerColor = tokens.surfaceGlass,
                            focusedBorderColor = tokens.mintAccent,
                            unfocusedBorderColor = tokens.borderGlass,
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false },
                        containerColor = tokens.surfaceElevated
                    ) {
                        NairobiLocations.PRESET_LOCATIONS.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text("📍 ${loc.name} · ${loc.description}", color = tokens.textPrimary, fontSize = 13.sp) },
                                onClick = {
                                    selectedLocation = loc
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Duration & Budget Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Est. Duration (hrs)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = durationHoursText,
                            onValueChange = { durationHoursText = it },
                            placeholder = { Text("3", color = tokens.textMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = tokens.surfaceGlass,
                                unfocusedContainerColor = tokens.surfaceGlass,
                                focusedBorderColor = tokens.mintAccent,
                                unfocusedBorderColor = tokens.borderGlass,
                                focusedTextColor = tokens.textPrimary,
                                unfocusedTextColor = tokens.textPrimary
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payment (KSh)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = payKshText,
                            onValueChange = { payKshText = it },
                            placeholder = { Text("2500", color = tokens.textMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("post_pay_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = tokens.surfaceGlass,
                                unfocusedContainerColor = tokens.surfaceGlass,
                                focusedBorderColor = tokens.mintAccent,
                                unfocusedBorderColor = tokens.borderGlass,
                                focusedTextColor = tokens.textPrimary,
                                unfocusedTextColor = tokens.textPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Escrow & Commission breakdown notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated)
                        .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🔒 Escrow Deposit Required on Acceptance:", fontSize = 11.sp, color = tokens.textMuted)
                            Text("KSh $payKsh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Worker Payout (after 10% platform fee):", fontSize = 11.sp, color = tokens.textMuted)
                            Text("KSh $netWorkerPayout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tokens.mintAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = "Description & Tasks",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Detail the requirements, tools provided, and gate instructions...", fontSize = 12.sp, color = tokens.textMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surfaceGlass,
                        unfocusedContainerColor = tokens.surfaceGlass,
                        focusedBorderColor = tokens.mintAccent,
                        unfocusedBorderColor = tokens.borderGlass,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Job Site Photos (Cloudinary CDN)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Site & Task Photos (Cloudinary CDN)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textSecondary
                        )
                        Text(
                            text = "Attach up to 3 photos to help workers estimate work (${uploadedPhotoUrls.size}/3)",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }

                    if (uploadedPhotoUrls.size < 3) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.mintContainer.copy(alpha = if (tokens.isDark) 0.35f else 0.8f))
                                .border(1.dp, tokens.mintAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isUploadingPhoto) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("add_job_photo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    color = tokens.mintAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Photo",
                                        tint = tokens.mintAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Add Photo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.mintAccent
                                    )
                                }
                            }
                        }
                    }
                }

                if (uploadedPhotoUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uploadedPhotoUrls) { photoUrl ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = CloudinaryService.getJobThumbnailUrl(photoUrl, 200, 200) ?: photoUrl,
                                    contentDescription = "Uploaded site photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Delete chip
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(tokens.coralAccent)
                                        .clickable { uploadedPhotoUrls.remove(photoUrl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                GlassButton(
                    text = "Post Job Live (Escrow Protected)",
                    onClick = {
                        val hours = durationHoursText.toIntOrNull() ?: 2
                        if (title.isNotBlank() && payKsh > 0) {
                            viewModel.createJob(
                                title = title.trim(),
                                category = selectedCategory,
                                locationName = selectedLocation.name,
                                latitude = selectedLocation.latitude,
                                longitude = selectedLocation.longitude,
                                payKsh = payKsh,
                                durationHours = hours,
                                description = description.trim(),
                                photoUrls = uploadedPhotoUrls.toList()
                            )
                        } else {
                            viewModel.showToast("Please enter a valid title and payment amount.")
                        }
                    },
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_job_button")
                )
            }
        }
    }

    // Interactive Google Map Pin Location Picker
    if (showLocationPickerMap) {
        LocationPickerMap(
            initialLocation = selectedLocation,
            onLocationConfirmed = { newLocation ->
                selectedLocation = newLocation
            },
            onDismiss = { showLocationPickerMap = false }
        )
    }
}
