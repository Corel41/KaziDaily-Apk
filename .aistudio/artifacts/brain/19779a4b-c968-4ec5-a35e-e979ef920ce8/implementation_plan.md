# Cloudinary Image Hosting & Mobile Media Pipeline for KaziDaily

Integrates Cloudinary as the media hosting and CDN backbone for KaziDaily, enabling workers and employers to upload and display optimized job site photos and artisan profile avatars with on-the-fly mobile transformations and real-time Firestore sync.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during the interactive interview:
> - **Primary Image Scope**: Job site photos (attached during gig posting/inspection) and artisan profile avatars (with live cropping and badge overlays).
> - **Hosting & Processing Engine**: **Cloudinary** (utilizing its automatic WebP/AVIF format optimization, dynamic quality scaling `q_auto`, and mobile CDN delivery).
> - **Android Media Ingestion**: Standard Zero-Permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) conforming to Google Play guidelines.
> - **Client Rendering Engine**: Jetpack Compose `AsyncImage` via Coil (`io.coil-kt:coil-compose:2.7.0`), leveraging Cloudinary URL transformations for bandwidth savings across 3G/4G connections in Kenya.

---

## 1. Overview & Core Concept

Currently, KaziDaily displays avatar initials (e.g., "BM") and category icons for jobs. By integrating **Cloudinary** alongside **Firestore**:
- **Artisans & Workers** can upload a real profile portrait directly from their phone camera or gallery, establishing identity trust before arriving at job sites.
- **Employers** can capture and upload up to 3 site photos when posting gigs (e.g., a burst pipe, damaged electrical box, or overgrown lawn), allowing workers to accurately estimate labor and equipment before applying.
- **Dynamic Mobile Optimization**: Cloudinary URL transforms automatically resize large 8MB smartphone photos down to exact thumbnail sizes (`w_200,h_200,c_fill,q_auto,f_auto`), drastically reducing mobile data consumption and latency.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Artisan Profile Avatar Upload**:
   - In the **Profile & Wallet** tab, tapping the avatar or edit badge triggers the Android Photo Picker (`PickVisualMedia`).
   - The selected image is uploaded asynchronously to Cloudinary using an upload preset with progress indication.
   - Upon completion, the resulting Cloudinary URL is saved to `/users/{uid}/photoUrl` in Firestore and immediately updates the app-wide avatar.

2. **Job Site Photos on Gig Creation**:
   - In **Post Job**, a new media card allows employers to select up to 3 photos of the job location or damaged item.
   - Thumbnails preview instantly with delete/replace chips.
   - Images upload to Cloudinary; image URLs are saved in the job's `photoUrls: List<String>` array in Firestore.

3. **Job Details & Exploration Image Carousel**:
   - On the **Jobs** list, job cards with photos feature a sleek 16:9 banner preview with a photo count chip (`📷 3`).
   - In the **Job Details Sheet**, users can swipe through full-width site photos with zoom/lightbox preview before submitting an application or funding escrow.

### Visual Styling & Theme Consistency
- Strictly follows existing Material 3 Glassmorphism tokens (`LocalGlassTokens`): rounded image borders (`RoundedCornerShape(16.dp)`), glass scrims, mint accent badges, and skeleton shimmer loaders while images stream over the network.
- Touch targets on image picker and delete chips strictly maintain the 48dp accessibility standard.

---

## 3. Key Product Decisions & Trade-Offs

- **Cloudinary Direct Upload vs. Local/Server Proxy**:
  - *Decision*: Direct upload from the Android client to Cloudinary via OkHttp REST endpoint using an unsigned preset (or configured cloud credentials in `.env`).
  - *Rationale*: Eliminates the need for a dedicated backend media server while keeping upload latency minimal for users.
- **Android Photo Picker vs. Legacy Storage Permissions**:
  - *Decision*: Zero-permission `ActivityResultContracts.PickVisualMedia`.
  - *Rationale*: Mandated by Google Play Developer Program policies; grants one-time secure access to the selected image without requiring intrusive storage or media permissions.
- **On-the-Fly Dynamic Resizing**:
  - *Decision*: Cloudinary URL transformations (`w_600,h_400,c_fill,g_auto,q_auto,f_auto`).
  - *Rationale*: Automatically delivers modern formats (WebP/AVIF) and reduces payload size by ~80% compared to raw camera JPEGs.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Android Client                                 │
│                                                                             │
│  ┌───────────────────────┐              ┌────────────────────────────────┐  │
│  │ Android Photo Picker  │              │     Jetpack Compose Views      │  │
│  │  (Zero Permissions)   │              │   • ProfileAvatar (Coil)       │  │
│  └──────────┬────────────┘              │   • JobSitePhotoCarousel       │  │
│             │ Selected URI              └────────────────▲───────────────┘  │
│             ▼                                            │                  │
│  ┌───────────────────────┐                               │                  │
│  │ CloudinaryMediaService│                               │ Dynamic CDN URL  │
│  │ (Upload & Transform)  │                               │ w_400,q_auto     │
│  └──────────┬────────────┘                               │                  │
│             │                                            │                  │
└─────────────┼────────────────────────────────────────────┼──────────────────┘
              │ Direct HTTP Upload                         │
              ▼                                            │
┌─────────────────────────────────────────┐                │
│         Cloudinary CDN Storage          │────────────────┘
│   • Image compression (WebP / AVIF)     │
│   • On-the-fly cropping & thumbnails    │
│   • Direct secure HTTPS asset URLs      │
└────────────────────┬────────────────────┘
                     │ Returns HTTPS Image URL
                     ▼
┌─────────────────────────────────────────┐
│        Cloud Firestore Backend          │
│   • /users/{uid} ──> photoUrl           │
│   • /jobs/{jobId} ──> photoUrls[]       │
└─────────────────────────────────────────┘
```

### Data Model & Firestore Schema Updates

1. **`jobs/{jobId}` Document**:
   - `photoUrls`: `List<String>` (Cloudinary HTTPS URLs of job site images)
2. **`users/{uid}` Document**:
   - `photoUrl`: `String` (Cloudinary HTTPS URL of artisan portrait)

### Dependencies
- Enable `libs.coil.compose` (`io.coil-kt:coil-compose:2.7.0`) in `app/build.gradle.kts`.
- Standard `okhttp3` (already in `app/build.gradle.kts`) for multipart direct uploads to Cloudinary API.

---

## 5. Phased Implementation Plan

- **Phase 1: Dependencies & Cloudinary Media Service**:
  - Uncomment `implementation(libs.coil.compose)` in `app/build.gradle.kts`.
  - Create `CloudinaryService.kt` with helper methods for uploading images via OkHttp and generating transformed URLs (`getAvatarUrl`, `getJobThumbnailUrl`, `getJobBannerUrl`).
  - Configure `CLOUDINARY_CLOUD_NAME` and `CLOUDINARY_UPLOAD_PRESET` via `BuildConfig` and `.env.example`.

- **Phase 2: UI Photo Picker & Image Upload Integration**:
  - Implement `AvatarImagePicker` composable in `ProfileWalletScreen` with `PickVisualMedia`.
  - Update `PostJobScreen` with a multi-image picker allowing employers to attach up to 3 photos.
  - Wire upload progress indicators and error snackbars.

- **Phase 3: Job Display & Firestore Persistence**:
  - Update `FirestoreJob` and `Job` models with `photoUrls: List<String>`.
  - Update `KaziFirestoreRepository` to persist and retrieve image URLs.
  - Enhance `JobCard` and `JobDetailsSheet` to display high-resolution image carousels with Coil async loaders and fallback placeholders.

- **Phase 4: Verification & Build Validation**:
  - Test image picker contracts in Robolectric unit tests.
  - Run `compile_applet` to confirm flawless compilation.
