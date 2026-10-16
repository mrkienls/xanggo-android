"""
XăngGo - Hệ Thống Crawler Backend Tự Động Đa Nguồn (Multi-Source Failover)
Thu thập giá xăng Việt Nam thời gian thực, tỷ giá quốc tế và chỉ số Sức mua (Affordability).
Đồng bộ vào Firebase Firestore và xuất file tĩnh (GitHub Raw CDN).
"""
import os
import sys
import json
import logging
from datetime import datetime
from zoneinfo import ZoneInfo
import requests
from bs4 import BeautifulSoup
import re

VN_TZ = ZoneInfo("Asia/Ho_Chi_Minh")

# Hỗ trợ UTF-8 cho Windows console
if sys.platform.startswith("win"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

# Thiết lập logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# Thư mục xuất dữ liệu tĩnh (dành cho CDN / Local Test)
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")
os.makedirs(OUTPUT_DIR, exist_ok=True)

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept-Language": "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7",
}

# -------------------------------------------------------------
# 1. THU THẬP BẢNG GIÁ XĂNG DẦU VIỆT NAM THỜI GIAN THỰC (2 NGUỒN DỰ PHÒNG)
# -------------------------------------------------------------
def fetch_vietnam_fuel_prices(previous_items=None):
    """
    Cào bảng giá xăng dầu thời gian thực với 2 nguồn tự động chuyển đổi (Failover):
    - Nguồn 1: vietnambiz.vn (Có sẵn giá Vùng 1 + mức tăng/giảm delta)
    - Nguồn 2 (Dự phòng): webgia.com/gia-xang-dau/petrolimex/ (Có sẵn giá Vùng 1 & Vùng 2 chuẩn Petrolimex)
    """
    prev_map = {}
    if previous_items:
        for it in previous_items:
            prev_map[it.get("id", "")] = it

    # --- NGUỒN 1: VietnamBiz ---
    logger.info("Đang kết nối Nguồn 1 (vietnambiz.vn)...")
    try:
        res = requests.get("https://vietnambiz.vn/gia-xang-dau-hom-nay.html", headers=HEADERS, timeout=12)
        if res.status_code == 200:
            soup = BeautifulSoup(res.text, "html.parser")
            table = soup.find("table")
            if table:
                items = parse_vietnambiz_table(table)
                if items and len(items) >= 3:
                    logger.info(f"Nguồn 1 thành công: {len(items)} mặt hàng xăng dầu mới nhất!")
                    return items
    except Exception as e:
        logger.warning(f"Nguồn 1 (vietnambiz.vn) gặp lỗi: {e}")

    # --- NGUỒN 2 (DỰ PHÒNG): WebGia Petrolimex ---
    logger.info("Đang chuyển sang Nguồn 2 dự phòng (webgia.com/gia-xang-dau/petrolimex/)...")
    try:
        res = requests.get("https://webgia.com/gia-xang-dau/petrolimex/", headers=HEADERS, timeout=12)
        if res.status_code == 200:
            soup = BeautifulSoup(res.text, "html.parser")
            table = soup.find("table")
            if table:
                items = parse_webgia_petrolimex_table(table, prev_map)
                if items and len(items) >= 3:
                    logger.info(f"Nguồn 2 (webgia.com) thành công: {len(items)} mặt hàng Petrolimex!")
                    return items
    except Exception as e:
        logger.warning(f"Nguồn 2 (webgia.com) gặp lỗi: {e}")

    logger.error("Cả 2 nguồn trực tuyến đều không phản hồi. Sẽ giữ dữ liệu thật của kỳ gần nhất.")
    return None


def parse_vietnambiz_table(table):
    """Trích xuất từng hàng của bảng giá kỳ điều hành từ VietnamBiz"""
    items = []
    rows = table.find_all("tr")
    for row in rows:
        cols = [td.get_text(strip=True) for td in row.find_all(["td", "th"])]
        if len(cols) >= 4 and any(char.isdigit() for char in cols[2]):
            name = normalize_fuel_display_name(cols[1])
            try:
                zone_1 = int("".join(filter(str.isdigit, cols[2])))
                if zone_1 < 10000 or zone_1 > 60000:
                    continue
                zone_2 = int(round(zone_1 * 1.02 / 10) * 10)

                raw_delta = cols[3].replace("đ", "").replace(".", "").replace(",", "").strip()
                delta = int(raw_delta) if raw_delta.lstrip("+-").isdigit() else 0

                item_id = generate_fuel_id(name)
                items.append({
                    "id": item_id,
                    "name": name,
                    "zone_1": zone_1,
                    "zone_2": zone_2,
                    "delta": delta,
                    "unit": "VND/lít",
                    "badge_color": get_fuel_color(name),
                    "is_featured": "95-iii" in name.lower()
                })
            except Exception as e:
                logger.debug(f"Bỏ qua hàng không hợp lệ: {e}")
                continue
    return items


def parse_webgia_petrolimex_table(table, prev_map):
    """Trích xuất bảng giá Petrolimex chuẩn Vùng 1 & Vùng 2 từ WebGia"""
    items = []
    rows = table.find_all("tr")
    for row in rows:
        cols = [td.get_text(strip=True) for td in row.find_all(["td", "th"])]
        if len(cols) >= 3 and any(char.isdigit() for char in cols[1]):
            raw_name = cols[0]
            name = normalize_fuel_display_name(raw_name)
            try:
                zone_1 = int("".join(filter(str.isdigit, cols[1])))
                zone_2_digits = "".join(filter(str.isdigit, cols[2]))
                zone_2 = int(zone_2_digits) if zone_2_digits else int(round(zone_1 * 1.02 / 10) * 10)
                if zone_1 < 10000 or zone_1 > 60000:
                    continue

                item_id = generate_fuel_id(name)
                prev_item = prev_map.get(item_id)
                if prev_item and prev_item.get("zone_1"):
                    diff = zone_1 - int(prev_item["zone_1"])
                    delta = diff if diff != 0 else int(prev_item.get("delta", 0))
                else:
                    delta = 0

                items.append({
                    "id": item_id,
                    "name": name,
                    "zone_1": zone_1,
                    "zone_2": zone_2,
                    "delta": delta,
                    "unit": "VND/lít",
                    "badge_color": get_fuel_color(name),
                    "is_featured": "95-iii" in name.lower()
                })
            except Exception:
                continue
    return items


def normalize_fuel_display_name(raw_name):
    n = raw_name.strip()
    low = n.lower()
    if "95" in low and ("mức 5" in low or "95-v" in low):
        return "Xăng RON 95-V"
    if "95" in low and ("mức 3" in low or "95-iii" in low):
        return "Xăng RON 95-III"
    if "e5" in low or "92" in low:
        return "Xăng E5 RON 92-II"
    if "0,001s" in low or "0.001s" in low:
        return "Dầu DO 0.001S-V"
    if "0,05s" in low or "0.05s" in low:
        return "Dầu DO 0.05S-II"
    if "dầu hỏa" in low:
        return "Dầu hỏa 2-K"
    return n.replace(",", ".")


def generate_fuel_id(name):
    clean = name.lower().replace("xăng", "").replace("dầu", "").strip()
    return clean.replace(" ", "_").replace("-", "_").replace(".", "").replace(",", "")


def get_fuel_color(name):
    n = name.lower()
    if "95-v" in n:
        return "#D97706"
    elif "95" in n:
        return "#F97316"
    elif "e5" in n or "92" in n:
        return "#16A34A"
    elif "do" in n or "diesel" in n:
        return "#475569"
    return "#0D9488"


def load_previous(filename):
    """Đọc dữ liệu thật của lần chạy thành công trước (backend/output/*.json được commit lại sau mỗi lần chạy)."""
    path = os.path.join(OUTPUT_DIR, filename)
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None


HISTORY_MAX_POINTS = 12


def build_price_history(previous_history, ron95, e5, do):
    """
    Nối thêm kỳ giá mới vào lịch sử đã lưu khi giá thay đổi (mỗi kỳ điều hành).
    """
    history = list(previous_history or [])
    last = history[-1] if history else None

    unchanged = last is not None and (
        last.get("ron95_price") == ron95 and
        last.get("e5_price") == e5 and
        last.get("diesel_price") == do
    )
    if unchanged:
        return history[-HISTORY_MAX_POINTS:]

    now = datetime.now(VN_TZ)
    history.append({
        "date": now.strftime("%d/%m"),
        "full_date": now.strftime("%d/%m/%Y"),
        "ron95_price": ron95,
        "e5_price": e5,
        "diesel_price": do,
        "delta_ron95": (ron95 - last["ron95_price"]) if last else 0
    })
    return history[-HISTORY_MAX_POINTS:]

# -------------------------------------------------------------
# 2. THU THẬP TỶ GIÁ & DỮ LIỆU SO SÁNH THẾ GIỚI
# -------------------------------------------------------------
COUNTRY_META = {
    "Libya": ("Libya", "🇱🇾", 18.0),
    "Iran": ("Iran", "🇮🇷", 15.0),
    "Venezuela": ("Venezuela", "🇻🇪", 6.0),
    "Angola": ("Angola", "🇦🇴", 8.0),
    "Kuwait": ("Kuwait", "🇰🇼", 95.0),
    "Algeria": ("Algeria", "🇩🇿", 12.0),
    "Turkmenistan": ("Turkmenistan", "🇹🇲", 22.0),
    "Egypt": ("Ai Cập", "🇪🇬", 10.0),
    "Qatar": ("Qatar", "🇶🇦", 180.0),
    "Bolivia": ("Bolivia", "🇧🇴", 11.0),
    "Oman": ("Oman", "🇴🇲", 55.0),
    "Saudi Arabia": ("Ả Rập Xê Út", "🇸🇦", 75.0),
    "Iraq": ("Iraq", "🇮🇶", 16.0),
    "Bahrain": ("Bahrain", "🇧🇭", 70.0),
    "Azerbaijan": ("Azerbaijan", "🇦🇿", 18.0),
    "Sudan": ("Sudan", "🇸🇩", 3.0),
    "Kazakhstan": ("Kazakhstan", "🇰🇿", 30.0),
    "Tunisia": ("Tunisia", "🇹🇳", 11.0),
    "Ecuador": ("Ecuador", "🇪🇨", 18.0),
    "Niger": ("Niger", "🇳🇪", 2.0),
    "Russia": ("Nga", "🇷🇺", 38.0),
    "Belarus": ("Belarus", "🇧🇾", 20.0),
    "Nigeria": ("Nigeria", "🇳🇬", 5.0),
    "Gabon": ("Gabon", "🇬🇦", 22.0),
    "Taiwan": ("Đài Loan", "🇹🇼", 90.0),
    "Ethiopia": ("Ethiopia", "🇪🇹", 3.0),
    "Maldives": ("Maldives", "🇲🇻", 35.0),
    "Indonesia": ("Indonesia", "🇮🇩", 13.5),
    "Vietnam": ("Việt Nam", "🇻🇳", 12.8),
    "Japan": ("Nhật Bản", "🇯🇵", 95.0),
    "Bhutan": ("Bhutan", "🇧🇹", 10.0),
    "Malaysia": ("Malaysia", "🇲🇾", 33.0),
    "India": ("Ấn Độ", "🇮🇳", 7.5),
    "DR Congo": ("CHDC Congo", "🇨🇩", 2.0),
    "Trinidad & Tobago": ("Trinidad & Tobago", "🇹🇹", 45.0),
    "UAE": ("UAE (Các Tiểu Vương Quốc)", "🇦🇪", 140.0),
    "Madagascar": ("Madagascar", "🇲🇬", 1.8),
    "USA": ("Hoa Kỳ (Mỹ)", "🇺🇸", 215.0),
    "Guyana": ("Guyana", "🇬🇾", 48.0),
    "Benin": ("Benin", "🇧🇯", 4.0),
    "Kyrgyzstan": ("Kyrgyzstan", "🇰🇬", 5.0),
    "Brazil": ("Brazil", "🇧🇷", 26.0),
    "Liberia": ("Liberia", "🇱🇷", 2.5),
    "Paraguay": ("Paraguay", "🇵🇾", 17.0),
    "Suriname": ("Suriname", "🇸🇷", 16.0),
    "Guatemala": ("Guatemala", "🇬🇹", 14.0),
    "Nepal": ("Nepal", "🇳🇵", 4.0),
    "Haiti": ("Haiti", "🇭🇹", 4.5),
    "Colombia": ("Colombia", "🇨🇴", 19.0),
    "Puerto Rico": ("Puerto Rico", "🇵🇷", 95.0),
    "Botswana": ("Botswana", "🇧🇼", 21.0),
    "Burundi": ("Burundi", "🇧🇮", 1.0),
    "Nicaragua": ("Nicaragua", "🇳🇮", 6.5),
    "Bangladesh": ("Bangladesh", "🇧🇩", 7.0),
    "El Salvador": ("El Salvador", "🇸🇻", 14.0),
    "Afghanistan": ("Afghanistan", "🇦🇫", 1.5),
    "Guinea": ("Guinea", "🇬🇳", 3.5),
    "China": ("Trung Quốc", "🇨🇳", 35.0),
    "Grenada": ("Grenada", "🇬🇩", 28.0),
    "Togo": ("Togo", "🇹🇬", 3.0),
    "Fiji": ("Fiji", "🇫🇯", 15.0),
    "Saint Lucia": ("Saint Lucia", "🇱🇨", 32.0),
    "South Korea": ("Hàn Quốc", "🇰🇷", 92.0),
    "Pakistan": ("Pakistan", "🇵🇰", 4.5),
    "Argentina": ("Argentina", "🇦🇷", 34.0),
    "Panama": ("Panama", "🇵🇦", 46.0),
    "Lebanon": ("Lebanon", "🇱🇧", 11.0),
    "Sri Lanka": ("Sri Lanka", "🇱🇰", 10.5),
    "Cameroon": ("Cameroon", "🇨🇲", 4.8),
    "Tanzania": ("Tanzania", "🇹🇿", 3.2),
    "Canada": ("Canada", "🇨🇦", 150.0),
    "Burkina Faso": ("Burkina Faso", "🇧🇫", 2.5),
    "Mozambique": ("Mozambique", "🇲🇿", 1.6),
    "Thailand": ("Thái Lan", "🇹🇭", 20.5),
    "Mauritius": ("Mauritius", "🇲🇺", 30.0),
    "Mali": ("Mali", "🇲🇱", 2.6),
    "Philippines": ("Philippines", "🇵🇭", 11.2),
    "Burma": ("Myanmar", "🇲🇲", 3.5),
    "Malta": ("Malta", "🇲🇹", 90.0),
    "Honduras": ("Honduras", "🇭🇳", 8.5),
    "Morocco": ("Ma-rốc", "🇲🇦", 10.0),
    "Chile": ("Chile", "🇨🇱", 42.0),
    "Curacao": ("Curacao", "🇨🇼", 50.0),
    "Cambodia": ("Campuchia", "🇰🇭", 5.8),
    "Dom. Rep.": ("Cộng hòa Dominica", "🇩🇴", 28.0),
    "Ivory Coast": ("Bờ Biển Ngà", "🇨🇮", 7.0),
    "Uzbekistan": ("Uzbekistan", "🇺🇿", 7.0),
    "Swaziland": ("Eswatini", "🇸🇿", 11.0),
    "Mexico": ("Mexico", "🇲🇽", 32.0),
    "Dominica": ("Dominica", "🇩🇲", 23.0),
    "Zambia": ("Zambia", "🇿🇲", 4.0),
    "Namibia": ("Namibia", "🇳🇦", 13.0),
    "Australia": ("Úc", "🇦🇺", 175.0),
    "Ghana": ("Ghana", "🇬🇭", 6.5),
    "Aruba": ("Aruba", "🇦🇼", 80.0),
    "Jamaica": ("Jamaica", "🇯🇲", 17.0),
    "Kenya": ("Kenya", "🇰🇪", 6.0),
    "Georgia": ("Georgia", "🇬🇪", 19.0),
    "Cayman Islands": ("Quần đảo Cayman", "🇰🇾", 230.0),
    "Syria": ("Syria", "🇸🇾", 3.0),
    "Costa Rica": ("Costa Rica", "🇨🇷", 36.0),
    "Peru": ("Peru", "🇵🇪", 20.0),
    "Senegal": ("Senegal", "🇸🇳", 4.5),
    "Uganda": ("Uganda", "🇺🇬", 2.8),
    "Turkey": ("Thổ Nhĩ Kỳ", "🇹🇷", 28.0),
    "Lesotho": ("Lesotho", "🇱🇸", 3.2),
    "Armenia": ("Armenia", "🇦🇲", 18.0),
    "Seychelles": ("Seychelles", "🇸🇨", 45.0),
    "Bahamas": ("Bahamas", "🇧🇸", 85.0),
    "Andorra": ("Andorra", "🇦🇩", 120.0),
    "N. Maced.": ("Bắc Macedonia", "🇲🇰", 19.0),
    "South Africa": ("Nam Phi", "🇿🇦", 18.0),
    "C. Afr. Rep.": ("Trung Phi", "🇨🇫", 1.5),
    "Bulgaria": ("Bulgaria", "🇧🇬", 38.0),
    "Sweden": ("Thụy Điển", "🇸🇪", 155.0),
    "Moldova": ("Moldova", "🇲🇩", 16.0),
    "Jordan": ("Jordan", "🇯🇴", 13.0),
    "Belize": ("Belize", "🇧🇿", 19.0),
    "Laos": ("Lào", "🇱🇦", 5.2),
    "Mongolia": ("Mông Cổ", "🇲🇳", 14.0),
    "Hungary": ("Hungary", "🇭🇺", 52.0),
    "Bosnia & Herz.": ("Bosnia & Herzegovina", "🇧🇦", 21.0),
    "New Zealand": ("New Zealand", "🇳🇿", 130.0),
    "Slovenia": ("Slovenia", "🇸🇮", 80.0),
    "Cyprus": ("Síp", "🇨🇾", 85.0),
    "Serbia": ("Serbia", "🇷🇸", 28.0),
    "Rwanda": ("Rwanda", "🇷🇼", 3.0),
    "Montenegro": ("Montenegro", "🇲🇪", 27.0),
    "Cuba": ("Cuba", "🇨🇺", 8.0),
    "Cape Verde": ("Cape Verde", "🇨🇻", 11.0),
    "Spain": ("Tây Ban Nha", "🇪🇸", 85.0),
    "Ukraine": ("Ukraine", "🇺🇦", 13.0),
    "Sierra Leone": ("Sierra Leone", "🇸🇱", 1.8),
    "Croatia": ("Croatia", "🇭🇷", 55.0),
    "Lithuania": ("Lithuania", "🇱🇹", 68.0),
    "Zimbabwe": ("Zimbabwe", "🇿🇼", 4.0),
    "Luxembourg": ("Luxembourg", "🇱🇺", 350.0),
    "Austria": ("Áo", "🇦🇹", 145.0),
    "Slovakia": ("Slovakia", "🇸🇰", 58.0),
    "Poland": ("Ba Lan", "🇵🇱", 55.0),
    "San Marino": ("San Marino", "🇸🇲", 130.0),
    "Czech Republic": ("Cộng hòa Séc", "🇨🇿", 75.0),
    "Barbados": ("Barbados", "🇧🇧", 50.0),
    "Romania": ("Romania", "🇷🇴", 45.0),
    "Wallis and Futuna": ("Wallis & Futuna", "🇼🇫", 35.0),
    "Latvia": ("Latvia", "🇱🇻", 60.0),
    "Estonia": ("Estonia", "🇪🇪", 75.0),
    "Ireland": ("Ireland", "🇮🇪", 280.0),
    "Belgium": ("Bỉ", "🇧🇪", 140.0),
    "Iceland": ("Iceland", "🇮🇸", 210.0),
    "Uruguay": ("Uruguay", "🇺🇾", 55.0),
    "Italy": ("Ý", "🇮🇹", 100.0),
    "UK": ("Vương quốc Anh", "🇬🇧", 135.0),
    "Portugal": ("Bồ Đào Nha", "🇵🇹", 70.0),
    "Albania": ("Albania", "🇦🇱", 19.0),
    "Germany": ("Đức", "🇩🇪", 140.0),
    "France": ("Pháp", "🇫🇷", 125.0),
    "Monaco": ("Monaco", "🇲🇨", 500.0),
    "Singapore": ("Singapore", "🇸🇬", 230.0),
    "Greece": ("Hy Lạp", "🇬🇷", 60.0),
    "Mayotte": ("Mayotte", "🇾🇹", 35.0),
    "Liechtenstein": ("Liechtenstein", "🇱🇮", 400.0),
    "Switzerland": ("Thụy Sĩ", "🇨🇭", 260.0),
    "Finland": ("Phần Lan", "🇫🇮", 145.0),
    "Israel": ("Israel", "🇮🇱", 140.0),
    "Netherlands": ("Hà Lan", "🇳🇱", 160.0),
    "Denmark": ("Đan Mạch", "🇩🇰", 190.0),
    "Norway": ("Na Uy", "🇳🇴", 240.0),
    "Malawi": ("Malawi", "🇲🇼", 1.8),
    "Hong Kong": ("Hong Kong", "🇭🇰", 145.0)
}

ASEAN_SET = {"Vietnam", "Singapore", "Malaysia", "Thailand", "Indonesia", "Philippines", "Cambodia", "Laos", "Burma", "Brunei"}

def fetch_global_fuel_data(vn_price_vnd=28250):
    logger.info("Đang cập nhật tỷ giá ngoại tệ và dữ liệu xăng 170 quốc gia toàn cầu...")
    
    prev_world = load_previous("world_fuel.json") or {}
    usd_to_vnd = float(prev_world.get("usd_to_vnd_rate") or 25800.0)
    try:
        fx_res = requests.get("https://open.er-api.com/v6/latest/USD", timeout=10)
        if fx_res.status_code == 200:
            rates = fx_res.json().get("rates", {})
            usd_to_vnd = rates.get("VND", usd_to_vnd)
            logger.info(f"Tỷ giá USD/VND hiện tại: {usd_to_vnd:,.0f} VND")
    except Exception as e:
        logger.warning(f"Lỗi lấy tỷ giá hối đoái: {e}. Dùng tỷ giá lần trước {usd_to_vnd:,.0f}")

    vn_price_usd = round(vn_price_vnd / usd_to_vnd, 2)

    scraped_countries = []
    try:
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
        r = requests.get('https://www.globalpetrolprices.com/gasoline_prices/', headers=headers, timeout=15)
        if r.status_code == 200:
            soup = BeautifulSoup(r.text, 'html.parser')
            g = soup.find('div', id='graphic')
            price_divs = []
            if g:
                for d in g.find_all('div'):
                    txt = d.get_text(strip=True)
                    if re.match(r'^\d+\.\d{3}$', txt):
                        price_divs.append(float(txt))

            step_prices = price_divs[::2]
            titles = [el.get_text(strip=True).replace('*', '').strip() for el in soup.find_all('div', class_='outsideTitleElement')]

            if len(titles) > 50 and len(step_prices) == len(titles):
                logger.info(f"Cào thành công {len(titles)} quốc gia từ GlobalPetrolPrices!")
                for title, price in zip(titles, step_prices):
                    meta = COUNTRY_META.get(title, (title, "🌐", 25.0))
                    vn_name, flag, daily_income = meta
                    is_asean = title in ASEAN_SET
                    is_vn = (title == "Vietnam")

                    actual_usd = vn_price_usd if is_vn else price
                    actual_vnd = int(vn_price_vnd) if is_vn else int(round(actual_usd * usd_to_vnd))
                    affordability = round((actual_usd / daily_income) * 100, 2)

                    note = "Kỳ điều hành hôm nay" if is_vn else ("Chính phủ trợ giá" if price < 0.5 else ("Thuế cao" if price > 2.2 else ""))
                    scraped_countries.append({
                        "code": title[:3].upper(),
                        "name": vn_name,
                        "flag": flag,
                        "region": "ASEAN" if is_asean else "Global",
                        "price_usd": actual_usd,
                        "price_vnd": actual_vnd,
                        "daily_income_usd": daily_income,
                        "affordability_percent": affordability,
                        "is_current": is_vn,
                        "note": note
                    })
    except Exception as e:
        logger.warning(f"Lỗi khi cào dữ liệu thế giới: {e}")

    now = datetime.now(VN_TZ)
    updated_at = now.strftime("%d/%m/%Y %H:%M")
    stale = False

    if not scraped_countries:
        previous = load_previous("world_fuel.json")
        if not previous or not previous.get("countries"):
            logger.error("Không cào được dữ liệu thế giới và chưa có dữ liệu cũ. Bỏ qua cập nhật thế giới.")
            return None
        logger.warning("Không cào được dữ liệu thế giới. Giữ dữ liệu thật của lần trước.")
        stale = True
        updated_at = previous.get("updated_at", updated_at)
        usd_to_vnd = previous.get("usd_to_vnd_rate", usd_to_vnd)
        vn_price_usd = round(vn_price_vnd / usd_to_vnd, 2)
        for c in previous["countries"]:
            c = dict(c)
            if c.get("is_current"):
                c["price_usd"] = vn_price_usd
                c["price_vnd"] = int(vn_price_vnd)
                income = c.get("daily_income_usd") or 0
                if income:
                    c["affordability_percent"] = round((vn_price_usd / income) * 100, 2)
            scraped_countries.append(c)

    sorted_countries = sorted(scraped_countries, key=lambda x: x["price_usd"])

    return {
        "updated_at": updated_at,
        "updated_epoch": int(now.timestamp()),
        "is_stale": stale,
        "usd_to_vnd_rate": usd_to_vnd,
        "total_countries": len(sorted_countries),
        "vn_rank": next((i + 1 for i, c in enumerate(sorted_countries) if c.get("is_current")), 30),
        "countries": sorted_countries
    }

# -------------------------------------------------------------
# 3. ĐỒNG BỘ VÀO FIRESTORE & XUẤT STATIC CDN
# -------------------------------------------------------------
def sync_data(vn_data, global_data):
    """Lưu trữ dữ liệu ra file tĩnh và đẩy lên Firebase (nếu có cấu hình). Bộ nào None thì bỏ qua, không ghi đè."""
    vn_file = os.path.join(OUTPUT_DIR, "vietnam_fuel.json")
    global_file = os.path.join(OUTPUT_DIR, "world_fuel.json")

    if vn_data is not None:
        with open(vn_file, "w", encoding="utf-8") as f:
            json.dump(vn_data, f, ensure_ascii=False, indent=2)
    if global_data is not None:
        with open(global_file, "w", encoding="utf-8") as f:
            json.dump(global_data, f, ensure_ascii=False, indent=2)

    logger.info(f"Đã xuất dữ liệu tĩnh ra {OUTPUT_DIR}")

    key_path = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")
    if os.path.exists(key_path) or os.environ.get("FIREBASE_SERVICE_ACCOUNT"):
        try:
            import firebase_admin
            from firebase_admin import credentials, firestore

            if not firebase_admin._apps:
                if os.environ.get("FIREBASE_SERVICE_ACCOUNT"):
                    cred_dict = json.loads(os.environ["FIREBASE_SERVICE_ACCOUNT"])
                    cred = credentials.Certificate(cred_dict)
                else:
                    cred = credentials.Certificate(key_path)
                firebase_admin.initialize_app(cred)

            db = firestore.client()
            if vn_data is not None:
                db.collection("vietnam_fuel").document("current").set(vn_data)
            if global_data is not None:
                db.collection("world_fuel").document("summary").set(global_data)
            logger.info("Đã đồng bộ thành công lên Firebase Cloud Firestore!")
        except Exception as e:
            logger.error(f"Lỗi khi đồng bộ Firebase: {e}")
    else:
        logger.info("Chưa cấu hình Firebase (serviceAccountKey.json). Đã lưu dữ liệu ở chế độ Local Static JSON.")


# -------------------------------------------------------------
# MAIN RUNNER
# -------------------------------------------------------------
def run():
    logger.info("=== BẮT ĐẦU TIẾN TRÌNH CRAWLER XĂNGGO ===")
    source_failed = False
    previous_vn = load_previous("vietnam_fuel.json")

    items = fetch_vietnam_fuel_prices((previous_vn or {}).get("items"))
    if items:
        hero_fuel = next((x for x in items if x.get("is_featured")), items[0])
        e5_fuel = next((x for x in items if "e5" in x.get("id", "") or "92" in x.get("id", "")), items[1] if len(items) > 1 else hero_fuel)
        do_fuel = next((x for x in items if "do" in x.get("id", "") or "diesel" in x.get("id", "")), items[2] if len(items) > 2 else hero_fuel)

        history = build_price_history(
            (previous_vn or {}).get("history"),
            ron95=hero_fuel["zone_1"],
            e5=e5_fuel["zone_1"],
            do=do_fuel["zone_1"]
        )

        now = datetime.now(VN_TZ)
        vn_data = {
            "updated_at": now.strftime("%d/%m/%Y %H:%M"),
            "updated_epoch": int(now.timestamp()),
            "announcement_title": f"Bảng giá xăng dầu ngày {now.strftime('%d/%m/%Y')}",
            "source": "Kỳ điều hành Liên Bộ Công Thương - Tài chính (Petrolimex)",
            "items": items,
            "history": history
        }
    else:
        source_failed = True
        vn_data = None
        items = (previous_vn or {}).get("items") or []
        if not items:
            logger.error("Không có dữ liệu Việt Nam (cả mới lẫn cũ). Dừng, không ghi đè gì.")
            sys.exit(1)
        hero_fuel = next((x for x in items if x.get("is_featured")), items[0])

    global_data = fetch_global_fuel_data(vn_price_vnd=hero_fuel["zone_1"])
    if global_data is None or global_data.get("is_stale"):
        source_failed = True

    sync_data(vn_data, global_data)
    
    print("\n" + "="*60)
    print("           KẾT QUẢ CÀO DỮ LIỆU THỜI GIAN THỰC")
    print("="*60)
    if vn_data:
        print(f"🕒 Thời gian: {vn_data['announcement_title']} ({vn_data['updated_at']})")
    else:
        print("⚠️  Giá Việt Nam: GIỮ DỮ LIỆU LẦN TRƯỚC (nguồn lỗi)")
    if global_data:
        print(f"💵 Tỷ giá USD/VND: {global_data['usd_to_vnd_rate']:,.0f} đ")
    print("-" * 60)
    print(f"{'TÊN MẶT HÀNG':<25} | {'VÙNG 1':<10} | {'VÙNG 2':<10} | {'BIẾN ĐỘNG':<10}")
    print("-" * 60)
    for it in items:
        sign = "+" if it['delta'] > 0 else ""
        delta_str = f"{sign}{it['delta']} đ" if it['delta'] != 0 else "0 đ"
        print(f"{it['name']:<25} | {it['zone_1']:>7,} đ | {it['zone_2']:>7,} đ | {delta_str:>9}")
    print("="*60)
    if global_data:
        stale_note = " (dữ liệu thế giới lần trước)" if global_data.get("is_stale") else ""
        print(f"🌍 Xếp hạng Việt Nam: #{global_data['vn_rank']} / {global_data['total_countries']} thế giới{stale_note}")
        print(f"🇻🇳 Sức mua xăng VN: {next(c['affordability_percent'] for c in global_data['countries'] if c.get('is_current'))}% thu nhập bình quân ngày")
    print("="*60 + "\n")

    if source_failed:
        logger.error("=== HOÀN TẤT NHƯNG CÓ NGUỒN LỖI: đã giữ dữ liệu lần trước ===")
        sys.exit(1)
    logger.info("=== HOÀN TẤT THÀNH CÔNG ===")


if __name__ == "__main__":
    run()
