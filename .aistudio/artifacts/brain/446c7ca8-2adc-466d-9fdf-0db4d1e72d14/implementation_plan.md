# Blueprint 3D HUD & Facility Wayfinding

An Android application that transforms project blueprint PDFs into interactive 2D markup canvases and extruded 3D spatial models. Features a real-time Heads-Up Display (HUD) with directional beacons, compass radar, and an illuminated 3D path to guide users from their virtual location to any room or equipment ID. Includes an integrated inspection camera with location-tagged photo saving, 3D text annotations & drawing tools (marker/highlighter), editable labels, and complete dark/light theme adaptation.

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan has been updated with your latest requirements:

- **Pathfinding Navigation System**: Search or input any room name or equipment ID (e.g., `HVAC-CH-01`, `Server Room B`), and the app calculates and displays an illuminated 3D floor path from your current virtual position directly to the destination.
- **Dedicated Camera & Photo Inspection Tab**: In-app camera tab to capture inspection photos. Photos are tagged with project and room location in their name (e.g., `Blueprint_HVAC_Room_302_20261105.jpg`), saved into the device's photo gallery, and viewable directly within the app alongside room notes.
- **Full Dark & Light Theme Switcher**: Dedicated theme switcher that smoothly adapts the entire UI, including the 3D blueprint engine (blueprint dark slate vs. high-contrast architectural paper light mode), menus, HUD dials, and tools.
- **3D Blueprint Editing & Annotation Tools**: Draw directly in both 2D and 3D perspectives with a Marker tool, Highlighter tool (semi-transparent glowing overlay), and placeable 3D Text Annotations anchored to specific rooms or coordinates. All annotations are persisted in the Room database with the project.
- **Dual-Mode 2D & 3D Rendering**: Instant toggle between high-fidelity 2D blueprint markup and extruded 3D spatial walkthrough with camera tilt, rotation, and elevation.
- **Bundled Projects + PDF Document Picker**: Includes preloaded facility blueprints (Industrial HVAC Plant & Commercial Tech Lab) and supports importing custom PDF blueprints via Android's native `PdfRenderer`.

---

## 1. Overview & Core Concept

Field engineers, facility managers, and technicians need to inspect physical spaces, mark up issues on the spot, capture photographic proof, and find their way through dense facilities without getting lost.

This app provides an all-in-one spatial toolset:
1. **Interactive 3D Blueprinting**: Converts blueprint floor plans into 3D rooms with extruded walls, equipment markers, and editable labels.
2. **Virtual Navigation & Pathfinding**: Lets users place or move their virtual location, search for a room or equipment piece, and follow a highlighted guidance path in the 3D model.
3. **Field Camera & Photo Documentation**: Snaps inspection photos, automatically embeds the location/room name into the file naming and metadata, registers the photo with the Android MediaStore (camera roll), and attaches it to the project's photo log.
4. **Drawing & 3D Text Markup**: Provides pencil/marker, highlighter, and floating 3D text sticky notes that attach to floor coordinates.
5. **Equipment Directory & Notes**: Searchable equipment catalog with direct "Navigate To" buttons, plus a dedicated global Notes tab.
6. **Adaptive Dark/Light Themes**: Supports high-contrast daytime outdoor light mode and low-glare dark blueprint mode.

---

## 2. User Experience & Visual Design

### Key Navigation Tabs & Screen Layout

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MAIN APP TOP APP BAR                            │
│  [Project: Central HVAC Plant ▼]      [Theme ☀/🌙]  [PDF Import] [GPS] │
└────────────────────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                       MAIN CONTENT AREA (BY TAB)                       │
│                                                                        │
│  [Tab 1: 3D HUD & Blueprint]                                           │
│  • 3D / 2D Canvas with Camera Orbit, Tilt, Pan, Zoom                   │
│  • HUD Overlay: Compass Radar, Target Beacon, Distance & Bearing       │
│  • Pathfinding Search Bar ("Go to Room 104 or PUMP-01...")            │
│  • Highlighted 3D floor path line connecting user to target            │
│  • Floating Tool Palette: Marker, Highlighter, 3D Text Pin, Eraser     │
│                                                                        │
│  [Tab 2: Equipment & Devices]                                          │
│  • Search & filterable catalog (Status: Operational, Warning, Offline) │
│  • Quick "Show on 3D Path" action buttons                              │
│                                                                        │
│  [Tab 3: Camera & Photos]                                              │
│  • Integrated viewfinder to snap photos for current room/equipment     │
│  • Photos saved to device camera roll with location in filename        │
│  • Visual photo gallery tied to blueprint locations                    │
│                                                                        │
│  [Tab 4: Notes Hub]                                                    │
│  • Dedicated notes tab: room logs, maintenance checklists, search      │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────────────────────┐
│    [🗺️ 3D Map]      [⚙️ Equipment]      [📷 Camera]      [📝 Notes]    │
│                        BOTTOM NAVIGATION BAR                           │
└────────────────────────────────────────────────────────────────────────┘
```

### Visual Themes & Design System

- **Dark Mode (Default Blueprint Aesthetic)**:
  - Background: Deep Navy Slate (`#0B132B` / `#1C2541`)
  - Blueprint Grid: Subtle Cyan mesh lines (`#1E3A5F`)
  - 3D Extruded Walls: High-contrast Slate with glowing cyan accents (`#00E5FF`)
  - 3D Navigation Path: Neon Pulsing Aqua/Green (`#00F5D4`) with directional arrows
  - HUD Reticle & Compass: Luminous Cyan with translucent dark glass container
- **Light Mode (Architectural Drafting Aesthetic)**:
  - Background: Clean Drafting Paper (`#F8F9FA` / `#FFFFFF`)
  - Blueprint Lines: Architectural Indigo & Cobalt (`#1D3557` / `#457B9D`)
  - 3D Extruded Walls: Crisp off-white panels with subtle shadow contours
  - 3D Navigation Path: Vivid Royal Blue (`#2A6F97`)
  - HUD Reticle & Compass: Deep Slate with semi-transparent frosted card
- **Interaction Polish**:
  - Smooth camera interpolation when selecting rooms or switching between 2D and 3D.
  - Interactive tap feedback on all equipment pins, room polygons, and drawing tools.

---

## 3. Key Product Decisions & Trade-Offs

### 1. 3D Engine & Path Rendering
- **Chosen Approach**: Custom Compose hardware-accelerated 3D vector projection engine with polygon extrusion, depth sorting, and animated path mesh rendering.
- **Why**: Zero heavyweight native engine overhead, silky-smooth 60fps performance on Android, zero C++ library compatibility issues, and complete reactive integration with Compose state.
- **Path Calculation**: Breadth-First / A* corridor navigation connecting virtual user node -> hallway waypoint -> destination room/equipment entrance.

### 2. Camera Integration & Gallery Storage
- **Chosen Approach**: Android `ActivityResultContracts.TakePicture()` with a file URI pointing to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` / app picture directory with automatic MediaStore scanning.
- **File Naming Standard**: `Blueprint_[ProjectName]_[RoomOrTag]_[Timestamp].jpg` (e.g. `Blueprint_HVAC_Plant_Chiller_Room_20261105_172530.jpg`).
- **Why**: Automatically places photos into the user's Android Photo Gallery / Camera Roll without requiring dangerous deprecated storage permissions, adhering 100% to Google Play Creator Program policies.

### 3. Annotation & 3D Text Markup Strategy
- **Chosen Approach**: Two-tier annotation storage:
  1. *Vector Stroke Layer*: Stores marker & highlighter strokes as normalized coordinate vectors with color, width, and alpha.
  2. *3D Text Anchors*: Stores world coordinates (X, Y, Z) and text labels, projecting them into screen space so they orbit and tilt dynamically with the 3D model.

### 4. Data Persistence
- **Chosen Approach**: Room SQLite database with reactive `Flow` observing projects, rooms, equipment, notes, captured photo references, and annotations.

---

## 4. Technical Architecture & Component Structure

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  MainActivity                                                          │
│   ├── MainAppScaffold (Theme Controller, Navigation Bar, Top Bar)      │
│   ├── BlueprintTab (2D/3D Canvas, 3D Projection, HUD, Path, Toolbar)   │
│   ├── EquipmentTab (Filterable cards, "Navigate" action triggers)      │
│   ├── CameraTab (In-app capture, location tagging, photo gallery)      │
│   ├── NotesTab (Global notes list, room filter, Markdown/text editor)  │
│   └── EditLabelSheet (Edit room/equipment name, type, status, notes)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Events / StateFlow)
┌───────────────────────────────────▼────────────────────────────────────┐
│                           VIEWMODEL LAYER                              │
│  BlueprintViewModel                                                    │
│   • AppThemeState (Dark / Light toggle)                                │
│   • Active Project & Navigation Target (Room / Equipment ID)           │
│   • 3D Camera State (Pitch, Yaw, Zoom, Virtual User Location)         │
│   • Pathfinding Engine (Node Graph -> Calculated 3D Path)              │
│   • Annotation Engine (Marker, Highlighter, 3D Text Notes)             │
│   • Camera & Photo Capture State (Location tag generator, gallery sync)│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                           REPOSITORY LAYER                             │
│  BlueprintRepository                                                   │
│   • Local Room Database (Projects, Rooms, Equipment, Notes, Photos)    │
│   • Built-in Sample Project Bundles (Industrial HVAC & Tech Lab)       │
│   • Native PdfRenderer (PDF -> High-Res Blueprint Bitmap Tiles)         │
│   • MediaStorePhotoManager (Save to Camera Roll with location tags)    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Step-by-Step Implementation Strategy

1. **Theme & Color System**: Update `Theme.kt`, `Color.kt`, and `strings.xml` with specialized dark/light blueprints color palettes and typography.
2. **Room Database & Data Layer**: Implement Room entities, DAOs, and repository for:
   - Projects (name, PDF URI, GPS origin, active theme)
   - Rooms (name, polygon boundaries, height, color, notes)
   - Equipment (tag, name, room ID, coordinates, status, specs)
   - Annotations (marker/highlighter strokes, 3D text pins)
   - Notes (title, content, room link, timestamp)
   - Photos (file URI, location name, timestamp)
3. **Sample Facility Data**: Provide rich pre-populated industrial blueprints (HVAC plant and Tech Lab) with interconnected rooms, equipment nodes, and pre-calculated paths.
4. **3D Projection & Path Engine**:
   - Camera matrix transform (perspective projection, orbit/yaw, pitch, zoom).
   - Room extrusion renderer with depth shading and wall lighting.
   - 3D floor path line renderer connecting virtual user coordinates to destination room/equipment.
   - HUD overlay: compass radar, directional arrow, distance meter, and targeting reticle.
5. **Editing Tools**:
   - Marker pen (opaque line drawing).
   - Highlighter (semi-transparent glowing broad stroke).
   - 3D text annotation tool (tap on floor -> input text -> anchor in 3D space).
   - Eraser & Undo support.
   - Tap-to-edit label bottom sheet for room and equipment properties.
6. **Camera & Photo Roll Integration**:
   - Camera capture button and photo preview.
   - Saves captured images into the device camera roll/Pictures directory using `MediaStore`.
   - File naming format: `Blueprint_[Project]_[Location]_[Timestamp].jpg`.
   - Photo gallery viewer showing location tags and associated notes.
7. **Equipment & Notes Screens**:
   - Equipment list with search, category filtering, and one-tap "Navigate" button.
   - Comprehensive Notes screen with search and room links.
8. **Verification**: Run `compile_applet` to ensure successful compilation and verify all user journeys.
