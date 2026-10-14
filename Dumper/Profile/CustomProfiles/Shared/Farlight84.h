#pragma once

#include "../../IProfile.h"

class Farlight84Profile : public IProfile
{
public:
	Farlight84Profile() = default;

	std::vector<std::string> GetSupportedGames() const override
	{
		return {
		    "com.miraclegames.farlight84",
		};
	}

	// TEMPORARY enumerate-probe v3: extended header sweep (in-match dump).
	uintptr_t GetGObjects() const override
	{
		const uintptr_t Base = GMemory->GetUnrealModule().GetStart();
		static const uintptr_t Offs[] = {
			0xCB74D78, 0xCB74DB0, 0xCB74DE8, 0xCB82778, 0xCB95248, 0xCB952A0,
			0xCB952D0, 0xCB952D8, 0xCB952E0, 0xCB95328, 0xCB95348, 0xCB95370,
			0xCB95380, 0xCB953A0, 0xCB953A8, 0xCB95448, 0xCB95528, 0xCEE7600,
			0xCEE7620, 0xCEEC0E8, 0xCEECC30, 0xCEECC50, 0xCEECCD0, 0xCF0B380,
			0xCF0B3A0, 0xCF0B3F0, 0xCF0B420, 0xCF0B650, 0xCF0B670, 0xCF252F0,
			0xD0AECB8, 0xD0AECD8, 0xD0AEDA8, 0xD0AEDC8, 0xD0AD278, 0xD0AD280,
			0xCE44140, 0xCEF3F40, 0xCE93158, 0xCE93328, 0xCE93330, 0xCE93348,
			0xCEE29B8, 0xCEE29C8, 0xCEE2C58, 0xCEE2C68, 0xCEE2EC8, 0xCEE2ED8,
			0xCEE2EE8, 0xCE6128, 0xD0C92E8, 0xD0C92F0, 0xCB83AE8, 0xD0B2F28,
			0xD0B2F38, 0xD0B2F48, 0xD134B28, 0xCE45220, 0xCB8B150, 0xCB8B350,
			0xD0706F0, 0xD025CC8, 0xD0583E0, 0xD066730, 0xD06E0C8, 0xCF1B308,
			0xCEEC680, 0xCEECBE0, 0xCEECC00, 0xCEECAB8, 0xCEECB90, 0xCEECBB0,
			0xCEE7978, 0xCEE7998, 0xCEE8200, 0xCEE8220, 0xCEE87E0, 0xCEEBD28,
			0xCEEBD48, 0xCEEBDF8, 0xCEEC080, 0xCEEC0A0, 0xCEECC80, 0xCEECCA0,
			0xCE93CE0, 0xCE93D00, 0xCE93E70, 0xCE93E90, 0xCE93EC0, 0xCE93EE0,
			0xCF252E0, 0xCBC44F8, 0xCBC4518, 0xCBC4730, 0xCBC4750, 0xCBC4960,
			0xCBC4980, 0xCBC7988, 0xCBC79A8, 0xCBD5410, 0xCEBF168, 0xCF0B440,
			0xCF2D620, 0xCF2D640, 0xCF2D7B0, 0xCBE5410, 0xCBC0368, 0xCBC44F8,
		};
		for (uintptr_t Off : Offs)
		{
			std::unique_ptr<IObjectsLayout> L;
			if (IProfile::ResolveGObjectsLayout(Base + Off, L))
			{
				GLogger.FmtWrite(ELogLevel::Info, "PROBE7 HIT fileoff=0x{:X}\n", Off);
				return Base + Off;
			}
		}
		GLogger.FmtWrite(ELogLevel::Info, "PROBE7 all miss\n");
		return 0;
	}

	// FNamePool header near file offset 0xCED0AA0 (Blocks table @ +0x20,
	// ~294 live entries, block0 = None/ByteProperty/IntProperty).
	uintptr_t GetGNames() const override
	{
		const uintptr_t Addr = GMemory->GetUnrealModule().GetStart() + 0xCED0AA0;
		GLogger.FmtWrite(ELogLevel::Info, "Farlight84: trying GNames @ 0x{:X}\n", Addr);
		return Addr;
	}
};
