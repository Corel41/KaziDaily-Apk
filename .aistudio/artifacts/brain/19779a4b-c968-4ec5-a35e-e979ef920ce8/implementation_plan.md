# Guest Browsing & Glass Login Bottom Sheet Implementation Plan

Remove compulsory login on startup so users can freely browse jobs, profiles, and escrow statuses on the Explore feed, and display a gorgeous glass-morphism login bottom sheet only when protected actions (applying or posting a job) are triggered.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Decisions**:
> - **Startup Experience**: Landing directly on the Explore feed as a guest with a subtle sign-in prompt in the top bar / banner.
> - **Action Triggered Login**: When a guest tries to apply for a job or post a job, a gorgeous glass-morphism login bottom sheet pops up.
> - **Freely Browsable Data**: Job listings, employer profiles, escrow statuses, and search are fully accessible without logging in.

---

### 1. Overview & Core Concept

- **What It Does**: Allows non-registered users to explore KaziDaily instantly without being blocked by an initial login wall. Protected actions (Applying, Posting, Wallet transactions) seamlessly trigger an authentication prompt.
- **Key Value**: Dramatically lowers user acquisition friction, allowing potential workers and employers to experience the app's utility before committing to sign-in.

### 2. User Experience & Visual Design

- **Startup Route**: App launches straight into `HomeScreen` (Explore feed) with guest session active by default (unless user previously signed in).
- **Subtle Sign-In Prompt**: A refined banner or action chip in the top header or profile tab inviting users to "Sign in to apply & post jobs".
- **Glass-Morphism Login Bottom Sheet**: 
  - Blurs background / semi-transparent glass card with subtle border and glow.
  - Prominent "Continue with Google" button and email/password alternative.
  - Dismissible or proceeds to the action once authenticated.
- **Action Interception**: Clicking "Apply for Job" or "Post Job" when unauthenticated intercepts the navigation, shows the glass login sheet, and remembers the pending action to resume upon successful sign-in.

### 3. Key Product Decisions & Architecture

- **State Management**: Add `isLoggedIn` (or `currentUser`) state to `KaziViewModel`. Default to guest mode (or cached Firebase Auth session).
- **Navigation & Guarding**: Intercept `onApply` and `onPostJob` in UI components; if not logged in, trigger `showLoginModal = true`.
- **UI Component**: Create `GlassLoginBottomSheet.kt` adhering to Material 3 and KaziDaily glass design tokens (`LocalGlassTokens`).

### 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────┐
│                     KaziViewModel                      │
│   (authState: FirebaseUser?, isGuestMode: Boolean)     │
└───────────┬────────────────────────────────┬───────────┘
            │                                │
            ▼                                ▼
┌───────────────────────┐        ┌───────────────────────┐
│   Explore / Feed      │        │    Action Trigger     │
│  (Freely browsable)   │        │ (Apply / Post Job)    │
└───────────┬───────────┘        └───────────┬───────────┘
            │                                │
            │ (Not Logged In)                │ (Guarded)
            ▼                                ▼
┌────────────────────────────────────────────────────────┐
│            GlassLoginBottomSheet (Dialog)              │
│  - Gorgeous glass blur, lock icon, Google Sign-In      │
└────────────────────────────────────────────────────────┘
```
