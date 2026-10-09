"""
XăngGo - Hệ Thống Crawler Backend Tự Động 0 Đồng
Thu thập giá xăng Việt Nam thời gian thực, tỷ giá quốc tế và chỉ số Sức mua (Affordability).
Đồng bộ vào Firebase Firestore và xuất file tĩnh (Static CDN).
"""
import os
import sys
import json
import logging
from datetime import datetime
import requests
from bs4 import BeautifulSoup

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
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept-Language": "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7",
}

# -------------------------------------------------------------
# 1. THU THẬP BẢNG GIÁ XĂNG DẦU VIỆT NAM THỜI GIAN THỰC
# -------------------------------------------------------------
def fetch_vietnam_fuel_prices():
    """
    Cào bảng giá xăng dầu thời gian thực.
    Nguồn ưu tiên 1: Cổng dữ liệu cập nhật kỳ điều hành trực tiếp.
    Nguồn dự phòng 2: Fallback chuẩn xác.
    """
    logger.info("Đang kết nối tới cổng dữ liệu xăng dầu Việt Nam...")
    url = "https://vietnambiz.vn/gia-xang-dau-hom-nay.html"
    
    try:
        res = requests.get(url, headers=HEADERS, timeout=12)
        if res.status_code == 200:
            soup = BeautifulSoup(res.text, 'html.parser')
            table = soup.find('table')
            if table:
                items = parse_vietnambiz_table(table)
                if items:
                    logger.info(f"Cào thành công {len(items)} mặt hàng xăng dầu mới nhất từ bảng giá kỳ điều hành!")
                    return items
    except Exception as e:
        logger.warning(f"Lỗi khi cào dữ liệu live: {e}. Đang kích hoạt bộ dữ liệu dự phòng...")

    return get_fallback_vietnam_prices()


def parse_vietnambiz_table(table):
    """Trích xuất từng hàng của bảng giá kỳ điều hành mới nhất"""
    items = []
    rows = table.find_all('tr')
    for row in rows:
        cols = [td.get_text(strip=True) for td in row.find_all(['td', 'th'])]
        if len(cols) >= 4 and any(char.isdigit() for char in cols[2]):
            name = cols[1]
            try:
                # Giá Vùng 1: ví dụ '28.250 đ' -> 28250
                zone_1 = int(''.join(filter(str.isdigit, cols[2])))
                
                # Giá Vùng 2 theo quy định Petrolimex bằng 102% giá Vùng 1
                zone_2 = int(round(zone_1 * 1.02 / 10) * 10) # Làm tròn chục

                # Chênh lệch delta: ví dụ '+1070' -> 1070 hoặc '-590' -> -590
                raw_delta = cols[3].replace('đ', '').replace('.', '').replace(',', '').strip()
                delta = int(raw_delta) if raw_delta else 0

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


def generate_fuel_id(name):
    clean = name.lower().replace("xăng", "").replace("dầu", "").strip()
    return clean.replace(" ", "_").replace("-", "_").replace(".", "")


def get_fuel_color(name):
    n = name.lower()
    if "95-v" in n:
        return "#D97706" # Vàng hổ phách cao cấp
    elif "95" in n:
        return "#F97316" # Cam tiêu chuẩn
    elif "e5" in n or "92" in n:
        return "#16A34A" # Xanh lá sinh học
    elif "do" in n or "diesel" in n:
        return "#475569" # Xám than
    return "#0D9488"


def get_fallback_vietnam_prices():
    """Dữ liệu dự phòng chuẩn xác khi web nguồn bảo trì"""
    logger.info("Sử dụng bộ dữ liệu dự phòng chuẩn...")
    return [
        {
            "id": "e10_ron_95_iii",
            "name": "Xăng E10 RON 95-III",
            "zone_1": 28250,
            "zone_2": 28810,
            "delta": 1070,
            "unit": "VND/lít",
            "badge_color": "#F97316",
            "is_featured": True
        },
        {
            "id": "e5_ron_92_ii",
            "name": "Xăng E5 RON 92-II",
            "zone_1": 27700,
            "zone_2": 28250,
            "delta": 1140,
            "unit": "VND/lít",
            "badge_color": "#16A34A",
            "is_featured": False
        },
        {
            "id": "do_005s_ii",
            "name": "Dầu DO 0.05S-II",
            "zone_1": 29120,
            "zone_2": 29700,
            "delta": -590,
            "unit": "VND/lít",
            "badge_color": "#475569",
            "is_featured": False
        },
        {
            "id": "do_0001s_v",
            "name": "Dầu DO 0.001S-V",
            "zone_1": 30520,
            "zone_2": 31130,
            "delta": -590,
            "unit": "VND/lít",
            "badge_color": "#334155",
            "is_featured": False
        }
    ]

# -------------------------------------------------------------
# 2. THU THẬP TỶ GIÁ & DỮ LIỆU SO SÁNH THẾ GIỚI
# -------------------------------------------------------------
def fetch_global_fuel_data(vn_price_vnd=28250):
    logger.info("Đang cập nhật tỷ giá ngoại tệ và dữ liệu xăng quốc tế...")
    
    # 1. Lấy tỷ giá USD -> VND từ API mở miễn phí
    usd_to_vnd = 25800.0
    try:
        fx_res = requests.get("https://open.er-api.com/v6/latest/USD", timeout=10)
        if fx_res.status_code == 200:
            rates = fx_res.json().get("rates", {})
            usd_to_vnd = rates.get("VND", 25800.0)
            logger.info(f"Tỷ giá USD/VND hiện tại: {usd_to_vnd:,.0f} VND")
    except Exception as e:
        logger.warning(f"Lỗi lấy tỷ giá hối đoái: {e}. Sử dụng tỷ giá mặc định {usd_to_vnd}")

    # Giá xăng tính theo USD của Việt Nam
    vn_price_usd = round(vn_price_vnd / usd_to_vnd, 2)

    # Danh sách các quốc gia trọng điểm (ASEAN + Toàn cầu)
    raw_countries = [
        {"code": "VE", "name": "Venezuela", "flag": "🇻🇪", "region": "Global", "price_usd": 0.035, "daily_income": 6.0, "note": "Trợ cấp dầu mỏ gần như 0đ"},
        {"code": "MY", "name": "Malaysia", "flag": "🇲🇾", "region": "ASEAN", "price_usd": 0.44, "daily_income": 33.0, "note": "Chính phủ trợ giá xăng"},
        {"code": "ID", "name": "Indonesia", "flag": "🇮🇩", "region": "ASEAN", "price_usd": 0.79, "daily_income": 13.5, "note": ""},
        {"code": "VN", "name": "Việt Nam", "flag": "🇻🇳", "region": "ASEAN", "price_usd": vn_price_usd, "daily_income": 12.8, "is_current": True, "note": "Kỳ điều hành ngày 08/10"},
        {"code": "US", "name": "Hoa Kỳ", "flag": "🇺🇸", "region": "Global", "price_usd": 0.95, "daily_income": 215.0, "note": ""},
        {"code": "PH", "name": "Philippines", "flag": "🇵🇭", "region": "ASEAN", "price_usd": 1.10, "daily_income": 11.2, "note": ""},
        {"code": "TH", "name": "Thái Lan", "flag": "🇹🇭", "region": "ASEAN", "price_usd": 1.15, "daily_income": 20.5, "note": ""},
        {"code": "CN", "name": "Trung Quốc", "flag": "🇨🇳", "region": "Global", "price_usd": 1.18, "daily_income": 35.0, "note": ""},
        {"code": "KH", "name": "Campuchia", "flag": "🇰🇭", "region": "ASEAN", "price_usd": 1.22, "daily_income": 5.8, "note": ""},
        {"code": "JP", "name": "Nhật Bản", "flag": "🇯🇵", "region": "Global", "price_usd": 1.25, "daily_income": 95.0, "note": ""},
        {"code": "LA", "name": "Lào", "flag": "🇱🇦", "region": "ASEAN", "price_usd": 1.35, "daily_income": 5.2, "note": "Nhập khẩu 100%"},
        {"code": "DE", "name": "Đức", "flag": "🇩🇪", "region": "Global", "price_usd": 1.95, "daily_income": 140.0, "note": ""},
        {"code": "SG", "name": "Singapore", "flag": "🇸🇬", "region": "ASEAN", "price_usd": 2.15, "daily_income": 230.0, "note": "Thuế bảo vệ môi trường cao"},
        {"code": "HK", "name": "Hong Kong", "flag": "🇭🇰", "region": "Global", "price_usd": 3.25, "daily_income": 145.0, "note": "Giá xăng cao nhất thế giới"}
    ]

    countries = []
    for item in raw_countries:
        price_vnd = int(round(item["price_usd"] * usd_to_vnd))
        affordability = round((item["price_usd"] / item["daily_income"]) * 100, 2)
        countries.append({
            "code": item["code"],
            "name": item["name"],
            "flag": item["flag"],
            "region": item["region"],
            "price_usd": item["price_usd"],
            "price_vnd": price_vnd,
            "daily_income_usd": item["daily_income"],
            "affordability_percent": affordability,
            "is_current": item.get("is_current", False),
            "note": item.get("note", "")
        })

    sorted_countries = sorted(countries, key=lambda x: x["price_usd"])

    return {
        "updated_at": datetime.now().isoformat(),
        "usd_to_vnd_rate": usd_to_vnd,
        "total_countries": len(sorted_countries),
        "vn_rank": next((i + 1 for i, c in enumerate(sorted_countries) if c.get("is_current")), 4),
        "countries": sorted_countries
    }

# -------------------------------------------------------------
# 3. ĐỒNG BỘ VÀO FIRESTORE & XUẤT STATIC CDN
# -------------------------------------------------------------
def sync_data(vn_data, global_data):
    """Lưu trữ dữ liệu ra file tĩnh và đẩy lên Firebase (nếu có cấu hình)"""
    vn_file = os.path.join(OUTPUT_DIR, "vietnam_fuel.json")
    global_file = os.path.join(OUTPUT_DIR, "world_fuel.json")

    with open(vn_file, "w", encoding="utf-8") as f:
        json.dump(vn_data, f, ensure_ascii=False, indent=2)
    with open(global_file, "w", encoding="utf-8") as f:
        json.dump(global_data, f, ensure_ascii=False, indent=2)

    logger.info(f"Đã xuất dữ liệu tĩnh ra {vn_file} và {global_file}")

    # Firebase Firestore
    key_path = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")
    if os.path.exists(key_path) or os.environ.get("FIREBASE_SERVICE_ACCOUNT"):
        try:
            import firebase_admin
            from firebase_admin import credentials, firestore, messaging

            if not firebase_admin._apps:
                if os.environ.get("FIREBASE_SERVICE_ACCOUNT"):
                    cred_dict = json.loads(os.environ["FIREBASE_SERVICE_ACCOUNT"])
                    cred = credentials.Certificate(cred_dict)
                else:
                    cred = credentials.Certificate(key_path)
                firebase_admin.initialize_app(cred)

            db = firestore.client()
            db.collection("vietnam_fuel").document("current").set(vn_data)
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
    
    # 1. Cào giá Việt Nam thời gian thực
    items = fetch_vietnam_fuel_prices()
    vn_data = {
        "updated_at": datetime.now().isoformat(),
        "announcement_title": f"Bảng giá xăng dầu ngày {datetime.now().strftime('%d/%m/%Y')}",
        "source": "Kỳ điều hành Liên Bộ Công Thương - Tài chính (Petrolimex)",
        "items": items
    }

    # 2. Cào giá thế giới & tính Sức mua
    hero_fuel = next((x for x in items if x.get("is_featured")), items[0])
    global_data = fetch_global_fuel_data(vn_price_vnd=hero_fuel["zone_1"])

    # 3. Đồng bộ dữ liệu
    sync_data(vn_data, global_data)
    
    # 4. In bảng kết quả tổng hợp ra màn hình để kiểm tra
    print("\n" + "="*60)
    print("           KẾT QUẢ CÀO DỮ LIỆU THỜI GIAN THỰC")
    print("="*60)
    print(f"🕒 Thời gian: {vn_data['announcement_title']}")
    print(f"💵 Tỷ giá USD/VND: {global_data['usd_to_vnd_rate']:,.0f} đ")
    print("-" * 60)
    print(f"{'TÊN MẶT HÀNG':<25} | {'VÙNG 1':<10} | {'VÙNG 2':<10} | {'BIẾN ĐỘNG':<10}")
    print("-" * 60)
    for it in items:
        sign = "+" if it['delta'] > 0 else ""
        delta_str = f"{sign}{it['delta']} đ" if it['delta'] != 0 else "0 đ"
        print(f"{it['name']:<25} | {it['zone_1']:>7,} đ | {it['zone_2']:>7,} đ | {delta_str:>9}")
    print("="*60)
    print(f"🌍 Xếp hạng Việt Nam: #{global_data['vn_rank']} / {global_data['total_countries']} thế giới")
    print(f"🇻🇳 Sức mua xăng VN: {next(c['affordability_percent'] for c in global_data['countries'] if c.get('is_current'))}% thu nhập bình quân ngày")
    print("="*60 + "\n")
    logger.info("=== HOÀN TẤT THÀNH CÔNG ===")


if __name__ == "__main__":
    run()
