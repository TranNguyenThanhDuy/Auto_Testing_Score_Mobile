import firebase_admin
from firebase_admin import credentials, db

# Khởi tạo Firebase chỉ 1 lần
if not firebase_admin._apps:
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://scoring-project-1b526-default-rtdb.firebaseio.com/'
    })

def get_nhan_xet(diem_str):
    try:
        diem_thuc = float(diem_str.split('|')[0])
        tong_cau = float(diem_str.split('|')[1])
        diem_so = diem_thuc / tong_cau * 10  # Quy về thang 10

        if diem_so < 5:
            return "Yếu"
        elif diem_so < 6.5:
            return "Trung bình"
        elif diem_so < 8:
            return "Khá"
        elif diem_so < 9:
            return "Giỏi"
        else:
            return "Xuất sắc"
    except Exception:
        return "Chưa rõ"

def push_ketqua(mssv, ma_so, ma_de, diem, nhan_xet=None):
    if nhan_xet is None:
        nhan_xet = get_nhan_xet(diem)

    ref = db.reference(f"SinhVien/{mssv}")
    ref.update({
        "ma_so": ma_so,
        "ma_de": ma_de,
        "diem": diem,
        "nhan_xet": nhan_xet
    })

    print(f"✅ Đã đẩy dữ liệu sinh viên {mssv} lên Realtime Database.")
