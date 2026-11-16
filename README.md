# Snowman Dance (Fabric 1.21.10, client-side)

Press **G** and your player performs a dance recreated by eye from your reference video
(the "How to dance to snowman" clip, about 4.5 s to 19.3 s). Press **G** again to stop.

> **Status: first draft, not yet compiled or tested in-game.**
> The animation maths and file parsing were tested outside Minecraft (21 checks, all pass).
> The Minecraft-facing code (key binding, model hook) was written from knowledge of the
> 1.21.x APIs without being able to compile against them. The first build may fail with a
> compile error; if it does, send the error log and it will be fixed.

## 1. Get the .jar (no installs needed, uses GitHub)

1. Create a new **empty repository** on github.com (private is fine).
2. Click **Add file > Upload files** and drag in **everything inside this folder**, including
   the `.github` folder (it contains the build recipe). Commit.
   *If your file browser hides folders that start with a dot (macOS: press Cmd+Shift+. in Finder),
   or the upload skips it, create the file by hand instead: **Add file > Create new file**, type
   `.github/workflows/build.yml` as the name, paste the contents of that file, commit.*
3. Open the **Actions** tab. The "Build mod" job starts by itself (takes a few minutes).
4. When it shows a green tick, open the run and download **mcdance-snowman-dance-jar** from
   *Artifacts*. Unzip it. Use the file named `mcdance-snowman-dance-1.0.0.jar`.
5. If the run is red, open it, copy the red error text and send it back.

Building locally instead: install JDK 21 and Gradle 8.14+, edit `loader_version` and
`fabric_version` in `gradle.properties` to current values from https://fabricmc.net/develop/
and run `gradle build`. The jar appears in `build/libs/`.

## 2. Install and play

1. Install Fabric Loader for Minecraft 1.21.10 (https://fabricmc.net/use/).
2. Put **Fabric API** (the 1.21.10 version) and `mcdance-snowman-dance-1.0.0.jar` in your
   `mods` folder.
3. Start the game, load a world, press **G**.

By default the camera switches to third-person front view so you can see the dance, and returns
to your previous view afterwards. (In first person only your hand is drawn, and it keeps its
normal animation.)

## 3. Dependencies

| Dependency | Version |
|---|---|
| Minecraft Java Edition | 1.21.10 |
| Fabric Loader | 0.17.x or newer |
| Fabric API | the 1.21.10 build (supplies the key-binding and tick hooks) |
| Java | 21 |

No animation library is used; the dance is applied directly to the player model.

## 4. Controls and settings

**Key:** change it in *Options > Controls > Key Binds > Snowman Dance* like any other key.
The config value `activationKey` only sets the *default* for a fresh install; a key you rebind in
the Controls screen takes priority.

**Config file:** `config/mcdance.json` (created on first launch).

| Setting | Default | Meaning |
|---|---|---|
| `activationKey` | `"G"` | Default key. Use names like `"G"`, `"F9"`, `"left.shift"`. |
| `loop` | `false` | `true`: repeat until you press the key again. `false`: play once, then return to normal. |
| `speed` | `1.0` | Playback speed (0.1 to 4.0). |
| `interruptible` | `true` | `true`: walking, jumping, swimming, taking damage or attacking cancels the dance. `false`: those are ignored. The dance key **always** stops the dance, so it can never get stuck. |
| `blendSeconds` | `0.35` | Fade time into and out of the dance (0 = instant). |
| `mirror` | `false` | Swap left and right. Use this if the dance is the wrong way round. |
| `yawOffsetDegrees` | `0` | Rotate the whole dance, for example `90` to change which way the character faces. |
| `switchToThirdPerson` | `true` | Auto-switch from first person while dancing. |
| `reloadAnimationOnStart` | `true` | Re-read the animation file each time the dance starts (see below). |

Restart the game after editing the config.

## 5. Refining the choreography (no rebuild needed)

The dance lives in `src/main/resources/assets/mcdance/animations/snowman_dance.json`.
Copy it to `config/mcdance/dance.json` in your Minecraft folder: if that file exists it is used
instead of the built-in one, and it is re-read every time you press G. Edit, save, press G, look.

Each keyframe has a time `t` in seconds and joint angles in degrees:

* `rootYaw` turns the whole body (negative = toward the character's left, 0 = facing the front camera)
* `bounce` extra hip drop for a small knee-bounce
* `torso` and `head`: `[pitch, yaw, roll]` (pitch + = forward/down, yaw + = turn right, roll + = lean left)
* `rArm`, `lArm`, `rLeg`, `lLeg`: `[xRot, yRot, zRot]` exactly like Minecraft model-part rotations
  (xRot negative swings forward; right arm zRot + goes outward, left arm zRot - goes outward)

Anything left out of a keyframe uses the neutral standing pose. Movement between keyframes is smoothed
automatically. Tell me things like "at 7 to 8 seconds the right arm should be higher" and I will edit it.

## 6. What this is, and its limits

* **Built from low-quality footage.** The clip is dark and the dancer is small and motion-blurred, so
  the keyframes were read by eye at 4 frames per second, not from automatic pose tracking. Expect
  the overall sequence and timing to be right and individual arm angles to need tuning.
* **Which arm is which is a guess** (the footage does not make it clear). If the dance looks mirrored, set `"mirror": true`.
* **Minecraft's player has no elbows, knees, hips or spine.** Bent arms become straight arms at an angle,
  crouches become leg splits plus a hip drop, and torso motion is a rigid lean/twist around the hips.
* **Single-player.** The dance is purely visual and client-side; other players would not see it.
* Mannequins or other player-model entities in the world may also show the dance pose while it is active.

## 7. Project layout

```
.github/workflows/build.yml          build recipe used by GitHub
build.gradle, settings.gradle, gradle.properties
src/main/java/dev/mcdance/
    DanceModClient.java              entry point, key binding, config + animation loading
    DanceController.java             start / stop / timing / interruption rules
    PoseApplier.java                 copies the pose onto the player model
    PoseSolver.java                  pose maths (torso pivot, hip drop, whole-body turn)
    DanceTimeline.java, DancePose.java, DanceConfig.java, Mat3.java, MiniJson.java
    mixin/PlayerModelMixin.java      hooks the player model after vanilla poses it
src/main/resources/                  fabric.mod.json, mixin config, lang file, snowman_dance.json
src/test/java/dev/mcdance/SelfCheck.java   plain-Java checks of the maths (not run by Gradle)
```
