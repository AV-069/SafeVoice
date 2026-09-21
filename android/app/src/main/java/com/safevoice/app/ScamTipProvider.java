package com.safevoice.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Static, rule-based tip text for the v4 live-detection warning screen.
 * NOT dynamic/AI-generated (locked decision). Tips are looked up by
 * category (from ScamKeywordEngine) and the currently selected language
 * (same SharedPreferences key used by ScamKeywordEngine and
 * ScamListenerService), so a single source of truth drives language
 * everywhere in the native code.
 *
 * NOTE: ta/te/ml/kn tips below are machine-translated, same caveat as
 * scam_keywords.json — need native-speaker review before shipping.
 */
public class ScamTipProvider {

    private static final String PREFS_NAME = "safevoice_prefs";
    private static final String PREFS_LANGUAGE_KEY = "language";
    private static final String DEFAULT_LANGUAGE = "en";

    // category -> language -> tip text
    private static final Map<String, Map<String, String>> TIPS = new HashMap<>();

    static {
        put("otp",
                "en", "Never share an OTP or verification code with anyone on a call — banks and government offices never ask for this.",
                "hi", "किसी को भी फोन पर ओटीपी या सत्यापन कोड न बताएं — बैंक और सरकारी दफ्तर कभी यह नहीं मांगते।",
                "ta", "ஓடிபி அல்லது சரிபார்ப்பு குறியீட்டை போனில் யாருடனும் பகிர வேண்டாம் — வங்கிகளும் அரசு அலுவலகங்களும் இதை ஒருபோதும் கேட்காது.",
                "te", "ఓటీపీ లేదా వెరిఫికేషన్ కోడ్‌ను ఫోన్‌లో ఎవరితోనూ పంచుకోవద్దు — బ్యాంకులు, ప్రభుత్వ కార్యాలయాలు దీన్ని ఎప్పుడూ అడగవు.",
                "ml", "ഒടിപിയോ വെരിഫിക്കേഷൻ കോഡോ ഫോണിൽ ആരുമായും പങ്കിടരുത് — ബാങ്കുകളും സർക്കാർ ഓഫീസുകളും ഇത് ഒരിക്കലും ചോദിക്കില്ല.",
                "kn", "ಒಟಿಪಿ ಅಥವಾ ಪರಿಶೀಲನಾ ಕೋಡ್ ಅನ್ನು ಫೋನ್‌ನಲ್ಲಿ ಯಾರೊಂದಿಗೂ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ — ಬ್ಯಾಂಕುಗಳು ಮತ್ತು ಸರ್ಕಾರಿ ಕಚೇರಿಗಳು ಇದನ್ನು ಎಂದಿಗೂ ಕೇಳುವುದಿಲ್ಲ.");

        put("digital_arrest",
                "en", "There is no such thing as \"digital arrest.\" Real police never arrest you over a phone or video call.",
                "hi", "\"डिजिटल अरेस्ट\" जैसी कोई चीज़ नहीं होती। असली पुलिस कभी फोन या वीडियो कॉल पर गिरफ्तार नहीं करती।",
                "ta", "\"டிஜிட்டல் கைது\" என்று எதுவும் இல்லை. உண்மையான காவல்துறை போன் அல்லது வீடியோ கால் மூலம் ஒருபோதும் கைது செய்யாது.",
                "te", "\"డిజిటల్ అరెస్ట్\" అనేది లేనే లేదు. నిజమైన పోలీసులు ఎప్పుడూ ఫోన్ లేదా వీడియో కాల్‌లో అరెస్ట్ చేయరు.",
                "ml", "\"ഡിജിറ്റൽ അറസ്റ്റ്\" എന്നൊന്ന് ഇല്ല. യഥാർത്ഥ പോലീസ് ഫോണിലോ വീഡിയോ കോളിലോ ഒരിക്കലും അറസ്റ്റ് ചെയ്യില്ല.",
                "kn", "\"ಡಿಜಿಟಲ್ ಅರೆಸ್ಟ್\" ಎಂಬುದು ಇಲ್ಲವೇ ಇಲ್ಲ. ನಿಜವಾದ ಪೊಲೀಸರು ಫೋನ್ ಅಥವಾ ವಿಡಿಯೋ ಕರೆಯಲ್ಲಿ ಎಂದಿಗೂ ಬಂಧಿಸುವುದಿಲ್ಲ.");

        put("kyc",
                "en", "Banks never update KYC over a phone call. Do not share your Aadhaar or account details.",
                "hi", "बैंक कभी भी फोन कॉल पर केवाईसी अपडेट नहीं करते। अपना आधार या खाता विवरण साझा न करें।",
                "ta", "வங்கிகள் ஒருபோதும் போன் கால் மூலம் கேஒய்சியை புதுப்பிக்காது. உங்கள் ஆதார் அல்லது கணக்கு விவரங்களை பகிர வேண்டாம்.",
                "te", "బ్యాంకులు ఎప్పుడూ ఫోన్ కాల్‌లో కేవైసీని అప్‌డేట్ చేయవు. మీ ఆధార్ లేదా ఖాతా వివరాలను పంచుకోవద్దు.",
                "ml", "ബാങ്കുകൾ ഫോൺ കോളിലൂടെ ഒരിക്കലും കെവൈസി അപ്ഡേറ്റ് ചെയ്യില്ല. നിങ്ങളുടെ ആധാർ അല്ലെങ്കിൽ അക്കൗണ്ട് വിവരങ്ങൾ പങ്കിടരുത്.",
                "kn", "ಬ್ಯಾಂಕುಗಳು ಫೋನ್ ಕರೆಯಲ್ಲಿ ಎಂದಿಗೂ ಕೆವೈಸಿ ಅಪ್‌ಡೇಟ್ ಮಾಡುವುದಿಲ್ಲ. ನಿಮ್ಮ ಆಧಾರ್ ಅಥವಾ ಖಾತೆ ವಿವರಗಳನ್ನು ಹಂಚಿಕೊಳ್ಳಬೇಡಿ.");

        put("refund_lottery",
                "en", "You cannot win a lottery you never entered, and real refunds never require you to pay first.",
                "hi", "जो लॉटरी आपने कभी खरीदी ही नहीं, वह आप नहीं जीत सकते, और असली रिफंड के लिए पहले भुगतान करने की ज़रूरत नहीं होती।",
                "ta", "நீங்கள் ஒருபோதும் வாங்காத லாட்டரியை நீங்கள் வெல்ல முடியாது, உண்மையான பணத்திரும்பப் பெறுதலுக்கு முதலில் பணம் செலுத்த வேண்டியதில்லை.",
                "te", "మీరు ఎప్పుడూ కొనని లాటరీని మీరు గెలవలేరు, నిజమైన రీఫండ్‌కు ముందుగా చెల్లించాల్సిన అవసరం లేదు.",
                "ml", "നിങ്ങൾ ഒരിക്കലും വാങ്ങാത്ത ലോട്ടറി നിങ്ങൾക്ക് നേടാനാവില്ല, യഥാർത്ഥ റീഫണ്ടിന് ആദ്യം പണം നൽകേണ്ട ആവശ്യമില്ല.",
                "kn", "ನೀವು ಎಂದಿಗೂ ಖರೀದಿಸದ ಲಾಟರಿಯನ್ನು ನೀವು ಗೆಲ್ಲಲು ಸಾಧ್ಯವಿಲ್ಲ, ನಿಜವಾದ ರೀಫಂಡ್‌ಗೆ ಮೊದಲು ಪಾವತಿಸಬೇಕಾಗಿಲ್ಲ.");

        put("remote_access",
                "en", "Never install AnyDesk or share your screen with someone who called you.",
                "hi", "आपको फोन करने वाले किसी व्यक्ति के कहने पर एनीडेस्क इंस्टॉल न करें या अपनी स्क्रीन शेयर न करें।",
                "ta", "உங்களை அழைத்த ஒருவரின் சொல்படி ஒருபோதும் AnyDesk நிறுவவோ உங்கள் திரையை பகிரவோ வேண்டாம்.",
                "te", "మీకు కాల్ చేసిన వ్యక్తి చెప్పినట్లు AnyDesk ఇన్‌స్టాల్ చేయవద్దు లేదా మీ స్క్రీన్‌ను షేర్ చేయవద్దు.",
                "ml", "നിങ്ങളെ വിളിച്ച ഒരാൾ പറയുന്നത് കേട്ട് ഒരിക്കലും AnyDesk ഇൻസ്റ്റാൾ ചെയ്യുകയോ സ്ക്രീൻ ഷെയർ ചെയ്യുകയോ ചെയ്യരുത്.",
                "kn", "ನಿಮಗೆ ಕರೆ ಮಾಡಿದ ವ್ಯಕ್ತಿಯ ಮಾತು ಕೇಳಿ ಎಂದಿಗೂ AnyDesk ಅನ್ನು ಸ್ಥಾಪಿಸಬೇಡಿ ಅಥವಾ ನಿಮ್ಮ ಸ್ಕ್ರೀನ್ ಅನ್ನು ಹಂಚಿಕೊಳ്ളಬೇಡಿ.");

        put("utility",
                "en", "Utility companies do not disconnect service over one phone call. Contact them using the number on your bill.",
                "hi", "बिजली/पानी कंपनियां एक फोन कॉल पर सेवा नहीं काटतीं। अपने बिल पर दिए नंबर से संपर्क करें।",
                "ta", "மின்/நீர் நிறுவனங்கள் ஒரு போன் கால் மூலம் சேவையை துண்டிக்காது. உங்கள் பில்லில் உள்ள எண்ணைப் பயன்படுத்தி தொடர்பு கொள்ளுங்கள்.",
                "te", "విద్యుత్/నీటి కంపెనీలు ఒక్క ఫోన్ కాల్‌తో సేవను నిలిపివేయవు. మీ బిల్లుపై ఉన్న నంబర్‌ను ఉపయోగించి సంప్రదించండి.",
                "ml", "വൈദ്യുതി/ജല കമ്പനികൾ ഒരൊറ്റ ഫോൺ കോളിൽ സേവനം വിച്ഛേദിക്കില്ല. നിങ്ങളുടെ ബില്ലിലെ നമ്പർ ഉപയോഗിച്ച് ബന്ധപ്പെടുക.",
                "kn", "ವಿದ್ಯುತ್/ನೀರಿನ ಕಂಪನಿಗಳು ಒಂದೇ ಫೋನ್ ಕರೆಯಲ್ಲಿ ಸೇವೆಯನ್ನು ಕಡಿತಗೊಳಿಸುವುದಿಲ್ಲ. ನಿಮ್ಮ ಬಿಲ್‌ನಲ್ಲಿರುವ ಸಂಖ್ಯೆಯನ್ನು ಬಳಸಿ ಸಂಪರ್ಕಿಸಿ.");

        put("secrecy",
                "en", "A real emergency is never a secret. If someone says to hide this call from family, hang up.",
                "hi", "असली आपातकाल कभी गुप्त नहीं होता। अगर कोई कहे कि यह कॉल परिवार से छुपाएं, तो फोन काट दें।",
                "ta", "உண்மையான அவசரநிலை ஒருபோதும் ரகசியமாக இருக்காது. இந்த அழைப்பை குடும்பத்திடமிருந்து மறைக்கச் சொன்னால், போனை துண்டியுங்கள்.",
                "te", "నిజమైన అత్యవసర పరిస్థితి ఎప్పుడూ రహస్యంగా ఉండదు. ఈ కాల్‌ను కుటుంబం నుండి దాచమని ఎవరైనా చెప్తే, ఫోన్ కట్ చేయండి.",
                "ml", "യഥാർത്ഥ അടിയന്തരാവസ്ഥ ഒരിക്കലും രഹസ്യമായിരിക്കില്ല. ഈ കോൾ കുടുംബത്തിൽ നിന്ന് മറയ്ക്കാൻ ആരെങ്കിലും പറഞ്ഞാൽ, ഫോൺ കട്ട് ചെയ്യുക.",
                "kn", "ನಿಜವಾದ ತುರ್ತು ಪರಿಸ್ಥಿತಿ ಎಂದಿಗೂ ರಹಸ್ಯವಾಗಿರುವುದಿಲ್ಲ. ಈ ಕರೆಯನ್ನು ಕುಟುಂಬದಿಂದ ಮರೆಮಾಡಲು ಯಾರಾದರೂ ಹೇಳಿದರೆ, ಫೋನ್ ಕಟ್ ಮಾಡಿ.");

        put("general",
                "en", "Stay alert. If anything feels urgent, secret, or frightening, hang up and call a family member.",
                "hi", "सतर्क रहें। अगर कुछ भी जल्दबाज़ी, गुप्त या डरावना लगे, तो फोन काटकर परिवार के किसी सदस्य को कॉल करें।",
                "ta", "எச்சரிக்கையாக இருங்கள். எதுவும் அவசரமாகவோ, ரகசியமாகவோ, பயமுறுத்துவதாகவோ தோன்றினால், போனை துண்டித்து குடும்ப உறுப்பினரை அழைக்கவும்.",
                "te", "అప్రమత్తంగా ఉండండి. ఏదైనా అత్యవసరంగా, రహస్యంగా లేదా భయానకంగా అనిపిస్తే, ఫోన్ కట్ చేసి కుటుంబ సభ్యుడికి కాల్ చేయండి.",
                "ml", "ജാഗ്രത പാലിക്കുക. എന്തെങ്കിലും അടിയന്തിരമോ രഹസ്യമോ ഭയപ്പെടുത്തുന്നതോ ആയി തോന്നിയാൽ, ഫോൺ കട്ട് ചെയ്ത് ഒരു കുടുംബാംഗത്തെ വിളിക്കുക.",
                "kn", "ಎಚ್ಚರಿಕೆಯಿಂದಿರಿ. ಏನಾದರೂ ತುರ್ತು, ರಹಸ್ಯ ಅಥವಾ ಭಯಾನಕವೆನಿಸಿದರೆ, ಫೋನ್ ಕಟ್ ಮಾಡಿ ಕುಟುಂಬ ಸದಸ್ಯರಿಗೆ ಕರೆ ಮಾಡಿ.");
    }

    private static void put(String category,
                            String enTip, String enText,
                            String hiTip, String hiText,
                            String taTip, String taText,
                            String teTip, String teText,
                            String mlTip, String mlText,
                            String knTip, String knText) {
        // Parameter names above are just for readability; args come in
        // pairs of (languageCode, text).
        Map<String, String> map = new HashMap<>();
        map.put(enTip, enText);
        map.put(hiTip, hiText);
        map.put(taTip, taText);
        map.put(teTip, teText);
        map.put(mlTip, mlText);
        map.put(knTip, knText);
        TIPS.put(category, map);
    }

    /** Exact locked wording — always shown first, same for every language for now. */
    public static String primaryTip() {
        return "This is likely a scammer — please hang up the phone or call a family member.";
    }

    /** Category-specific tip in the user's currently selected language. */
    public static String categoryTip(Context context, String category) {
        Map<String, String> byLanguage = TIPS.get(category != null ? category : "general");
        if (byLanguage == null) byLanguage = TIPS.get("general");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(PREFS_LANGUAGE_KEY, DEFAULT_LANGUAGE);
        if (lang == null || lang.isEmpty()) lang = DEFAULT_LANGUAGE;

        String tip = byLanguage.get(lang);
        return tip != null ? tip : byLanguage.get(DEFAULT_LANGUAGE);
    }
}