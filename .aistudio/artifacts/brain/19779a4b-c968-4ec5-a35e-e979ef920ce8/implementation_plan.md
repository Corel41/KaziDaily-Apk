# GitHub Patch Updates & Full-Screen In-Place Updater

Enables in-place patch updates for KaziDaily directly from GitHub releases without full uninstallation or data loss, featuring an immediate full-screen glass update experience that prompts a quick restart.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during the interactive interview:
> - **Patch Updates over Check Updates**: Replaced the generic "Check Updates" button with **"Patch Updates"** in the profile and settings UI. Updates fetch release patches and delta packages published on the GitHub repository.
> - **In-Place Installation (Zero Data Loss)**: Updates are installed in-place via the Android Package Installer. The app is never uninstalled; all user accounts, wallet balance, active escrow transactions, messages, and settings are 100% preserved.
> - **Immediate Full-Screen Experience**: When an update or patch is initiated, an immersive full-screen glass modal displays the patch changelog, download progress bar, and a prominent prompt: **"Patch Ready! Restart to Apply"** with a one-tap restart & apply action.

---

## 1. Overview & Core Concept

Currently, KaziDaily uses a small popup dialog with standard "Check Updates" phrasing. Users who want incremental patches rather than full reinstallation can now:
1. Tap **"Patch Updates"** in **Profile & Wallet** to query the GitHub repository for release patches and update artifacts.
2. Experience an immediate, polished **Full-Screen Patch Update** overlay adhering to KaziDaily's AMOLED dark / Frosted white glass aesthetics.
3. Review release highlights, patch fixes, and download delta package size before applying.
4. Watch real-time download and verification progress.
5. On completion, receive an immediate prompt to **"Restart & Apply Patch"**, launching the in-place package installer without uninstallation.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Triggering Patch Updates**:
   - In `ProfileWalletScreen`, the action card reads **"KaziDaily Patch Manager"** with current build details (`v1.0.0 Build 1 · GitHub Release Channel`).
   - The primary button reads **"🚀 Patch Updates"**.

2. **Immediate Full-Screen Experience (`UpdateDialog.kt`)**:
   - Renders as a full-screen edge-to-edge glass overlay (`DialogProperties(usePlatformDefaultWidth = false)`).
   - Top banner: GitHub release tag badge (`⚡ GitHub Patch Release v1.2.0`), release date, and delta package size.
   - Patch notes list with mint accent bullet indicators detailing fixes, escrow upgrades, and performance patches.
   - Reassurance chip: *"🛡️ In-Place Update: Retains all wallet funds, escrow state, and active chats."*

3. **Live Download & Restart Prompt**:
   - Animated linear progress bar with percentage, downloaded MB, and speed telemetry.
   - When finished: transitions to **"Patch Ready! Restart to Apply"** state with an accent button: **"🔄 Restart & Apply Now"**.
   - Tapping it triggers Android's package installer to install the patch on top of the existing app and relaunches seamlessly.

### Visual Styling & Theme Consistency
- Strictly adheres to Material 3 Glass tokens (`LocalGlassTokens`), rounded cards (`24.dp`), mint/purple accents, and AMOLED black/frosted background.
- Touch targets strictly comply with the 48dp accessibility standard.

---

## 3. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                             Android Client                             │
│                                                                        │
│   ┌───────────────────────────┐      ┌──────────────────────────────┐  │
│   │   ProfileWalletScreen     │      │   PatchUpdateFullScreen      │  │
│   │   "🚀 Patch Updates"      │      │   (Full-screen Glass Dialog) │  │
│   └─────────────┬─────────────┘      └──────────────▲───────────────┘  │
│                 │                                   │                  │
│                 ▼                                   │                  │
│   ┌─────────────────────────────────────────────────┴───────────────┐  │
│   │                   KaziViewModel & UpdateState                   │  │
│   │   • Checking ──> Available ──> Downloading ──> ReadyToRestart   │  │
│   └─────────────────────────────┬───────────────────────────────────┘  │
│                                 │                                      │
│                                 ▼                                      │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │               UpdateManager (GitHub Patch Engine)               │  │
│   │   • Fetches GitHub Release Patch Metadata & Delta APK           │  │
│   │   • Downloads to app cache without wiping user data             │  │
│   │   • Launches in-place Android PackageInstaller via FileProvider │  │
│   └─────────────────────────────┬───────────────────────────────────┘  │
└─────────────────────────────────┼──────────────────────────────────────┘
                                  │ HTTPS API
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   GitHub Repository Releases                           │
│   • Releases API / Assets: kazidaily-v1.2.0-patch.apk                  │
│   • Changelog & Release Notes                                          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Phased Implementation Plan

- **Phase 1: UpdateManager GitHub Patch Engine**:
  - Update `UpdateManager.kt` to reference GitHub release patches with accurate patch metadata, release notes, and delta package sizing.
  - Implement restart intent / in-place package install trigger preserving all user data.

- **Phase 2: Full-Screen Immediate Patch UI (`UpdateDialog.kt`)**:
  - Convert `UpdateDialog` into an edge-to-edge full-screen glass overlay.
  - Add GitHub release badges, detailed patch notes, data-preservation guarantees, and real-time progress indicators.
  - Implement the **"Restart & Apply Now"** immediate prompt view.

- **Phase 3: Profile Screen Refactoring**:
  - Update `ProfileWalletScreen.kt` to label the section as **"KaziDaily Patch Manager"** and replace "Check Updates" with **"🚀 Patch Updates"**.

- **Phase 4: Verification & Compilation**:
  - Run `compile_applet` to ensure seamless compilation without errors.
