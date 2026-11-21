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

test("Unauthenticated user: denied across user profile, tasks, transactions, and family_spaces", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Unauthorized",
      category: "TASK",
      priority: "HIGH",
      dueDateLabel: "Today",
      timeLabel: "09:00 AM",
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("family_spaces").where("memberUids", "array-contains", ALICE_UID).get());
});

test("User profile & subcollections: owner create, partial update, query, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      email: "alice@example.com",
      language: "bn",
      planTier: "FREE",
      monthlyBudgetBdt: 25000,
      offlineSmartMode: true,
      darkTheme: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "en",
      monthlyBudgetBdt: 30000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field or immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());

  // Alice creates task (NO id field), finance tx, memory, health record, smart device
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Morning Fajr & Study",
      category: "ROUTINE",
      priority: "HIGH",
      dueDateLabel: "Today",
      timeLabel: "06:00 AM",
      completed: false,
      notes: "Focus on Chapter 3",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject extra `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "Bad ID",
      category: "TASK",
      priority: "LOW",
      dueDateLabel: "Today",
      timeLabel: "10:00 AM",
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "Grocery Bazar",
      amount: 1450.5,
      txType: "EXPENSE",
      category: "Food",
      dateLabel: "21 Nov",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      title: "Preferred Language",
      content: "Responds best with Bangla + English medical terms.",
      memoryType: "PREFERENCE",
      pinned: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("smart_devices").doc("dev_1").set({
      userId: ALICE_UID,
      name: "Living Room Lamp",
      room: "Living Room",
      deviceType: "LIGHT",
      powerOn: true,
      level: 80,
      automationRule: "Turn on at 6:30 PM",
      createdAt: past,
      updatedAt: past,
    })
  );

  // List subcollections succeed for Alice, fail for Bob
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Family Spaces: member-only list queries, join by inviteCode, and denormalized items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("family_spaces").doc("FAM2026").set({
      name: "Rahman Family",
      ownerId: ALICE_UID,
      inviteCode: "FAM2026",
      memberUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Unfiltered list fails; filtered query succeeds for member Alice
  await assertFails(aliceDb.collection("family_spaces").get());
  await assertSucceeds(
    aliceDb.collection("family_spaces").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Bob cannot get FAM2026 before joining
  await assertFails(bobDb.collection("family_spaces").doc("FAM2026").get());

  // Bob joins FAM2026 by adding himself to memberUids
  await assertSucceeds(
    bobDb.collection("family_spaces").doc("FAM2026").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now Bob can query and add a family item
  await assertSucceeds(
    bobDb.collection("family_spaces").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("family_spaces").doc("FAM2026").collection("items").doc("item_1").set({
      spaceId: "FAM2026",
      authorId: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      title: "Buy Rice & Cooking Oil",
      itemType: "SHOPPING",
      completed: false,
      details: "5kg Miniket",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice can list and toggle the shared item
  await assertSucceeds(
    aliceDb.collection("family_spaces").doc("FAM2026").collection("items")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("family_spaces").doc("FAM2026").collection("items").doc("item_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
