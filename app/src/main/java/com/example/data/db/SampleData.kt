package com.example.data.db

import com.example.data.model.EquipmentEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectNoteEntity
import com.example.data.model.RoomEntity

object SampleData {

    val sampleProjects = listOf(
        ProjectEntity(
            id = "proj-hvac-plant",
            name = "Apex Central HVAC & Energy Plant",
            description = "Industrial mechanical blueprint with high-pressure chillers, steam boilers, and SCADA control hub.",
            pdfName = "HVAC_Mechanical_Floor_Plan_Rev3.pdf",
            gpsLat = 37.774929,
            gpsLng = -122.419416,
            compassDeclination = 13.2f
        ),
        ProjectEntity(
            id = "proj-tech-lab",
            name = "Metro Quantum Robotics Facility",
            description = "Cleanroom complex, AI server racks, robotics prototyping lab, and high-voltage UPS substation.",
            pdfName = "Metro_Tech_Lab_Schematic_A1.pdf",
            gpsLat = 40.712776,
            gpsLng = -74.005974,
            compassDeclination = -12.8f
        )
    )

    fun getRoomsForProject(projectId: String): List<RoomEntity> {
        return when (projectId) {
            "proj-hvac-plant" -> listOf(
                RoomEntity(
                    id = "room-hvac-1",
                    projectId = projectId,
                    name = "Primary Chiller Bay",
                    code = "CH-100",
                    floorNumber = 1,
                    minX = 0.08f, minY = 0.08f, maxX = 0.46f, maxY = 0.48f,
                    wallHeight = 35f,
                    colorHex = "#00E5FF",
                    notes = "Houses centrifugal water chillers 1 & 2. Vibration dampeners inspected quarterly."
                ),
                RoomEntity(
                    id = "room-hvac-2",
                    projectId = projectId,
                    name = "Boiler & Steam Chamber",
                    code = "BL-102",
                    floorNumber = 1,
                    minX = 0.52f, minY = 0.08f, maxX = 0.92f, maxY = 0.48f,
                    wallHeight = 38f,
                    colorHex = "#FFD166",
                    notes = "Gas supply valve shutoff located at north wall exterior. Wear heat safety gear."
                ),
                RoomEntity(
                    id = "room-hvac-3",
                    projectId = projectId,
                    name = "SCADA & Telemetry Hub",
                    code = "CTL-201",
                    floorNumber = 1,
                    minX = 0.08f, minY = 0.54f, maxX = 0.38f, maxY = 0.90f,
                    wallHeight = 30f,
                    colorHex = "#06D6A0",
                    notes = "Primary facility automation monitors and redundant backup UPS terminal."
                ),
                RoomEntity(
                    id = "room-hvac-4",
                    projectId = projectId,
                    name = "Chemical Treatment & Dosing",
                    code = "CHM-105",
                    floorNumber = 1,
                    minX = 0.44f, minY = 0.54f, maxX = 0.68f, maxY = 0.90f,
                    wallHeight = 32f,
                    colorHex = "#A78BFA",
                    notes = "Glycol and biocidal cooling water inhibitor additive pumps."
                ),
                RoomEntity(
                    id = "room-hvac-5",
                    projectId = projectId,
                    name = "480V Switchgear Substation",
                    code = "ELEC-110",
                    floorNumber = 1,
                    minX = 0.72f, minY = 0.54f, maxX = 0.92f, maxY = 0.90f,
                    wallHeight = 32f,
                    colorHex = "#F43F5E",
                    notes = "Main power bus bars. Arc flash protection category 4 required."
                )
            )
            "proj-tech-lab" -> listOf(
                RoomEntity(
                    id = "room-tech-1",
                    projectId = projectId,
                    name = "ISO Class 5 Cleanroom",
                    code = "CR-101",
                    floorNumber = 1,
                    minX = 0.08f, minY = 0.08f, maxX = 0.48f, maxY = 0.48f,
                    wallHeight = 32f,
                    colorHex = "#00E5FF",
                    notes = "Airlock gowning required before entering. Laminar airflow hoods active."
                ),
                RoomEntity(
                    id = "room-tech-2",
                    projectId = projectId,
                    name = "AI Datacenter Vault",
                    code = "SVR-102",
                    floorNumber = 1,
                    minX = 0.54f, minY = 0.08f, maxX = 0.92f, maxY = 0.48f,
                    wallHeight = 35f,
                    colorHex = "#60A5FA",
                    notes = "Liquid cooling loops for 16 compute racks. FM200 fire suppression active."
                ),
                RoomEntity(
                    id = "room-tech-3",
                    projectId = projectId,
                    name = "Robotics Prototyping Bay",
                    code = "ROB-201",
                    floorNumber = 1,
                    minX = 0.08f, minY = 0.54f, maxX = 0.48f, maxY = 0.90f,
                    wallHeight = 35f,
                    colorHex = "#F59E0B",
                    notes = "Overhead 2-ton gantry crane. Optical safety perimeter lasers enabled."
                ),
                RoomEntity(
                    id = "room-tech-4",
                    projectId = projectId,
                    name = "UPS & Battery Storage",
                    code = "UPS-205",
                    floorNumber = 1,
                    minX = 0.54f, minY = 0.54f, maxX = 0.92f, maxY = 0.90f,
                    wallHeight = 30f,
                    colorHex = "#10B981",
                    notes = "Modular lithium-iron-phosphate battery cabinets. Continuous thermal monitoring."
                )
            )
            else -> emptyList()
        }
    }

    fun getEquipmentForProject(projectId: String): List<EquipmentEntity> {
        return when (projectId) {
            "proj-hvac-plant" -> listOf(
                EquipmentEntity(
                    id = "eq-1",
                    projectId = projectId,
                    roomId = "room-hvac-1",
                    roomName = "Primary Chiller Bay",
                    tag = "CHILLER-01",
                    name = "Trane 500-Ton Centrifugal Chiller",
                    category = "HVAC",
                    posX = 0.22f, posY = 0.24f, posZ = 0f,
                    status = "Operational",
                    specifications = "Refrigerant R-134a, 480V 3-Phase, 420 kW, Delta-T: 12°F",
                    notes = "Condenser tubes brushed during last month's scheduled shutdown. Head pressure optimal."
                ),
                EquipmentEntity(
                    id = "eq-2",
                    projectId = projectId,
                    roomId = "room-hvac-1",
                    roomName = "Primary Chiller Bay",
                    tag = "CHILLER-02",
                    name = "Carrier Evergreen Screw Chiller",
                    category = "HVAC",
                    posX = 0.36f, posY = 0.32f, posZ = 0f,
                    status = "Maintenance",
                    specifications = "Variable Speed Drive, R-513A, 350-Ton, 2800 RPM",
                    notes = "Oil filter differential pressure alert. Scheduled for technician service on Friday."
                ),
                EquipmentEntity(
                    id = "eq-3",
                    projectId = projectId,
                    roomId = "room-hvac-1",
                    roomName = "Primary Chiller Bay",
                    tag = "PUMP-CHW-01",
                    name = "Primary Chilled Water Pump A",
                    category = "PLUMBING",
                    posX = 0.16f, posY = 0.40f, posZ = 0f,
                    status = "Operational",
                    specifications = "Bell & Gossett 1510, 60 HP, 1200 GPM @ 110 ft head",
                    notes = "Mechanical seal inspected; zero leakage. Motor bearing temp at 52°C."
                ),
                EquipmentEntity(
                    id = "eq-4",
                    projectId = projectId,
                    roomId = "room-hvac-2",
                    roomName = "Boiler & Steam Chamber",
                    tag = "BOILER-B1",
                    name = "Cleaver-Brooks 250HP Steam Boiler",
                    category = "HVAC",
                    posX = 0.68f, posY = 0.22f, posZ = 0f,
                    status = "Operational",
                    specifications = "150 PSI Design Pressure, Natural Gas / Dual Fuel, 8600 lbs/hr steam",
                    notes = "Stack temperature at 320°F. Combustion efficiency certified at 84.6%."
                ),
                EquipmentEntity(
                    id = "eq-5",
                    projectId = projectId,
                    roomId = "room-hvac-2",
                    roomName = "Boiler & Steam Chamber",
                    tag = "BOILER-B2",
                    name = "Standby Water Boiler B2",
                    category = "HVAC",
                    posX = 0.82f, posY = 0.36f, posZ = 0f,
                    status = "Offline",
                    specifications = "Rapid Fire Modulating 150HP, Low-NOx Burner",
                    notes = "Valved off for seasonal standby. Blowdown valves locked out."
                ),
                EquipmentEntity(
                    id = "eq-6",
                    projectId = projectId,
                    roomId = "room-hvac-3",
                    roomName = "SCADA & Telemetry Hub",
                    tag = "SCADA-PLC-01",
                    name = "Honeywell Experion Main PLC Rack",
                    category = "NETWORK",
                    posX = 0.22f, posY = 0.72f, posZ = 0f,
                    status = "Operational",
                    specifications = "Dual Redundant CP900 Controllers, Modbus TCP/IP, BACnet MSTP",
                    notes = "Firmware v4.81 up to date. All 96 remote I/O sensor channels active."
                ),
                EquipmentEntity(
                    id = "eq-7",
                    projectId = projectId,
                    roomId = "room-hvac-4",
                    roomName = "Chemical Treatment & Dosing",
                    tag = "CHEM-DOSE-03",
                    name = "Automated Biocide Dosing Skid",
                    category = "SENSOR",
                    posX = 0.56f, posY = 0.72f, posZ = 0f,
                    status = "Operational",
                    specifications = "Pulse-proportional diaphragm pump, ORP sensor controller",
                    notes = "Chemical tank at 78% volume. ORP setpoint reading at 680mV."
                ),
                EquipmentEntity(
                    id = "eq-8",
                    projectId = projectId,
                    roomId = "room-hvac-5",
                    roomName = "480V Switchgear Substation",
                    tag = "SWGR-480V",
                    name = "Square D Main 480V Distribution Board",
                    category = "ELECTRICAL",
                    posX = 0.82f, posY = 0.72f, posZ = 0f,
                    status = "Operational",
                    specifications = "3000A Main Circuit Breaker, Digitrip 1150 Trip Unit, 65kA IC",
                    notes = "Infrared thermal scan completed with zero hot spots identified."
                )
            )
            "proj-tech-lab" -> listOf(
                EquipmentEntity(
                    id = "eq-101",
                    projectId = projectId,
                    roomId = "room-tech-1",
                    roomName = "ISO Class 5 Cleanroom",
                    tag = "CR-AHU-01",
                    name = "Cleanroom Precision Air Handler",
                    category = "HVAC",
                    posX = 0.26f, posY = 0.26f, posZ = 0f,
                    status = "Operational",
                    specifications = "99.999% ULPA Filtration, 0.05 in. w.g. positive room pressure",
                    notes = "Particle count verification passed at 32 particles/m³ (>0.5µm)."
                ),
                EquipmentEntity(
                    id = "eq-102",
                    projectId = projectId,
                    roomId = "room-tech-2",
                    roomName = "AI Datacenter Vault",
                    tag = "SVR-RACK-A1",
                    name = "NVIDIA DGX H100 GPU Cluster Rack",
                    category = "NETWORK",
                    posX = 0.68f, posY = 0.22f, posZ = 0f,
                    status = "Operational",
                    specifications = "40kW Thermal Density, In-Row Direct-to-Chip Liquid Cooling",
                    notes = "Inlet water temp 24°C, return temp 38°C. Pump flow rate stable."
                ),
                EquipmentEntity(
                    id = "eq-103",
                    projectId = projectId,
                    roomId = "room-tech-3",
                    roomName = "Robotics Prototyping Bay",
                    tag = "ROBOT-ARM-01",
                    name = "KUKA KR QUANTEC 6-Axis Robot",
                    category = "SAFETY",
                    posX = 0.28f, posY = 0.72f, posZ = 0f,
                    status = "Maintenance",
                    specifications = "210kg Payload Capacity, 2700mm Reach, KRC4 Controller",
                    notes = "Joint 3 harmonic drive resolver calibration scheduled."
                ),
                EquipmentEntity(
                    id = "eq-104",
                    projectId = projectId,
                    roomId = "room-tech-4",
                    roomName = "UPS & Battery Storage",
                    tag = "UPS-MOD-01",
                    name = "Schneider Galaxy VM 250kVA Modular UPS",
                    category = "ELECTRICAL",
                    posX = 0.74f, posY = 0.72f, posZ = 0f,
                    status = "Operational",
                    specifications = "Li-ion Battery Strings, 99% ECOnversion efficiency mode",
                    notes = "Battery state of health: 98.4%. Autonomous runtime: 28 minutes at full load."
                )
            )
            else -> emptyList()
        }
    }

    fun getNotesForProject(projectId: String): List<ProjectNoteEntity> {
        return when (projectId) {
            "proj-hvac-plant" -> listOf(
                ProjectNoteEntity(
                    id = "note-1",
                    projectId = projectId,
                    title = "Annual Steam Boiler Certification Inspection",
                    content = "State pressure vessel inspector verified safety relief valves and low water cutoff test. Signed log on board.",
                    category = "Inspection",
                    linkedRoomName = "Boiler & Steam Chamber",
                    linkedEquipmentTag = "BOILER-B1"
                ),
                ProjectNoteEntity(
                    id = "note-2",
                    projectId = projectId,
                    title = "Chiller 02 Vibration & Oil Filter Alert",
                    content = "High differential across oil filter detected by telemetry hub. Replacement synthetic oil and 5-micron filter staged in chemical room.",
                    category = "Maintenance",
                    linkedRoomName = "Primary Chiller Bay",
                    linkedEquipmentTag = "CHILLER-02"
                ),
                ProjectNoteEntity(
                    id = "note-3",
                    projectId = projectId,
                    title = "Switchgear Arc Flash Labeling Upgrades",
                    content = "All electrical panels re-tagged with updated incident energy calculations per NFPA 70E standards.",
                    category = "Safety",
                    linkedRoomName = "480V Switchgear Substation",
                    linkedEquipmentTag = "SWGR-480V"
                )
            )
            "proj-tech-lab" -> listOf(
                ProjectNoteEntity(
                    id = "note-101",
                    projectId = projectId,
                    title = "Cleanroom Particle Certification",
                    content = "Semi-annual airborne particle count completed. ISO Class 5 certification renewed for next 6 months.",
                    category = "Inspection",
                    linkedRoomName = "ISO Class 5 Cleanroom",
                    linkedEquipmentTag = "CR-AHU-01"
                ),
                ProjectNoteEntity(
                    id = "note-102",
                    projectId = projectId,
                    title = "Liquid Cooling Manifold Pressure Check",
                    content = "Datacenter in-row secondary loop pressurized to 45 PSI. Zero glycol seepage detected across quick-disconnect fittings.",
                    category = "Specs",
                    linkedRoomName = "AI Datacenter Vault",
                    linkedEquipmentTag = "SVR-RACK-A1"
                )
            )
            else -> emptyList()
        }
    }
}
