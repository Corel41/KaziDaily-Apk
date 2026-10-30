const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated callers cannot read or create jobs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("jobs").get());
  await assertFails(
    unauthDb.collection("jobs").doc("job_1").set({
      title: "Test Job",
      employerId: ALICE_UID,
      status: "OPEN"
    })
  );
});

test("Signed-in user can create a job they own and read jobs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("jobs").doc("job_1").set({
      title: "Cleaning Job",
      category: "CLEANING",
      payKsh: 2500,
      locationName: "Kilimani",
      latitude: -1.2884,
      longitude: 36.7830,
      employerId: ALICE_UID,
      status: "OPEN"
    })
  );
  await assertSucceeds(aliceDb.collection("jobs").doc("job_1").get());
});

test("Alice cannot read or write Bob's wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("wallet").doc("summary").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("wallet").doc("summary").set({
      userId: BOB_UID,
      availableBalanceKsh: 5000,
      lockedInEscrowKsh: 0
    })
  );
});

test("Alice can read and write her own wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet").doc("summary").set({
      userId: ALICE_UID,
      availableBalanceKsh: 4500,
      lockedInEscrowKsh: 1200
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet").doc("summary").get()
  );
});

test("Only thread participants can read the thread", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("threads").doc("thread_1").set({
      jobId: "job_1",
      participantUids: [ALICE_UID, BOB_UID]
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();

  await assertSucceeds(aliceDb.collection("threads").doc("thread_1").get());
  await assertFails(charlieDb.collection("threads").doc("thread_1").get());
});
