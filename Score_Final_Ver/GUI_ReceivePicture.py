import tkinter as tk
from tkinter import filedialog, messagebox
import subprocess
import threading
import os
import time
from flask import Flask, request, jsonify
from werkzeug.serving import make_server
import json
from firebase_helper import push_ketqua

# --- Flask setup ---
app = Flask(__name__)
UPLOAD_FOLDER = 'received_images'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Đây sẽ là biến toàn cục tham chiếu đến GUI instance, để Flask lấy tham số answers và csv từ GUI hiện tại
app_gui = None

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'image' not in request.files:
        return "No image file in request", 400

    image = request.files['image']
    filename = image.filename
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    image.save(filepath)

    # Thông báo đã nhận ảnh lên GUI
    app_gui.notify_image_received(f"📸 Đã nhận ảnh: {filename}")

    # Lấy đường dẫn answers.json và tên file CSV hiện tại từ GUI
    answers_file = app_gui.answers_path.get()
    output_csv = app_gui.output_csv.get()

    if not answers_file or not os.path.isfile(answers_file):
        err_msg = "File answers.json chưa được chọn hoặc không tồn tại!"
        app_gui.add_result_text(err_msg)
        return jsonify({'error': err_msg}), 400

    if not output_csv:
        err_msg = "Tên file CSV lưu kết quả chưa được nhập!"
        app_gui.add_result_text(err_msg)
        return jsonify({'error': err_msg}), 400

    # Đợi tối đa 5 giây để file ảnh sẵn sàng
    wait_time = 0
    while (not os.path.exists(filepath) or os.path.getsize(filepath) == 0) and wait_time < 5:
        time.sleep(0.5)
        wait_time += 0.5

    if not os.path.exists(filepath) or os.path.getsize(filepath) == 0:
        err_msg = f"Ảnh chưa được lưu đúng hoặc bị lỗi: {filename}"
        app_gui.add_result_text(err_msg)
        return jsonify({'error': err_msg}), 500

    # Ảnh sẵn sàng, gọi subprocess chấm điểm(chamthi.py)
    try:
        completed = subprocess.run(
            ['python', 'chamthi.py',
             '--image', filepath,
             '--answers', answers_file,
             '--output', output_csv],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore',
            check=True
        )
        output = completed.stdout or ""
        app_gui.add_result_text(output)
        # ✅ Đọc lại sbd từ file tạm
        try:
            with open("last_sbd.txt", "r", encoding="utf-8") as f:
                sbd = f.read().strip()
        except Exception as e:
            print("❌ Không đọc được last_sbd.txt:", e)
            sbd = "unknown"

        print("📤 SBD gửi về client:", sbd)
        return jsonify({'result': output, 'sbd': sbd}), 200

    except subprocess.CalledProcessError as e:
        err = e.stderr or "Lỗi không rõ"
        app_gui.add_result_text(f"Lỗi chạy chamthi.py: {err}")
        return jsonify({'error': err}), 500


class FlaskThread(threading.Thread):
    def __init__(self, app):
        threading.Thread.__init__(self)
        self.server = make_server('0.0.0.0', 5000, app)
        self.ctx = app.app_context()
        self.ctx.push()
        self.daemon = True

    def run(self):
        print("Flask server đang chạy tại http://0.0.0.0:5000")
        self.server.serve_forever()

    def shutdown(self):
        self.server.shutdown()


# --- Tkinter GUI ---

class AppGUI:
    def __init__(self, root):
        self.root = root
        root.title("Chấm điểm GUI + Flask server")
        root.geometry("600x400")

        self.answers_path = tk.StringVar()
        self.output_csv = tk.StringVar(value="grading_result.csv")

        tk.Label(root, text="Chọn file đáp án (answers.json):").pack(anchor='w', padx=10, pady=5)
        frame_ans = tk.Frame(root)
        frame_ans.pack(fill='x', padx=10)
        tk.Entry(frame_ans, textvariable=self.answers_path).pack(side='left', fill='x', expand=True)
        tk.Button(frame_ans, text="Chọn...", command=self.browse_answers).pack(side='left', padx=5)

        tk.Label(root, text="Tên file CSV lưu kết quả:").pack(anchor='w', padx=10, pady=5)
        tk.Entry(root, textvariable=self.output_csv).pack(fill='x', padx=10)

        tk.Button(root, text="Chấm điểm (chạy chamthi.py trực tiếp)", command=self.run_grading_thread).pack(pady=15)

        tk.Label(root, text="Kết quả chấm điểm:").pack(anchor='w', padx=10)
        self.result_box = tk.Text(root, height=12, font=("Courier", 11))
        self.result_box.pack(fill='both', padx=10, pady=10)
        self.result_box.insert(tk.END, "📋 Kết quả sẽ hiển thị ở đây...\n")
        self.result_box.config(state='disabled')

    def browse_answers(self):
        filename = filedialog.askopenfilename(filetypes=[("JSON files", "*.json")])
        if filename:
            self.answers_path.set(filename)

    def run_grading_thread(self):
        threading.Thread(target=self.run_grading, daemon=True).start()

    def run_grading(self):
        answers_file = self.answers_path.get()
        csv_file = self.output_csv.get()

        if not answers_file:
            messagebox.showwarning("Thiếu file", "Vui lòng chọn file answers.json!")
            return
        if not csv_file:
            messagebox.showwarning("Thiếu tên CSV", "Vui lòng nhập tên file CSV!")
            return
        if not os.path.isfile("chamthi.py"):
            messagebox.showerror("Lỗi", "Không tìm thấy file 'chamthi.py'!")
            return

        try:
            completed = subprocess.run(
                ["python", "chamthi.py", "--answers", answers_file, "--output", csv_file, "--image", "received_images/sample.jpg"],
                capture_output=True,
                text=True,
                encoding='utf-8',
                errors='ignore',
                check=True
            )
            output = completed.stdout or ""
            self.add_result_text(output)
        except subprocess.CalledProcessError as e:
            err = e.stderr or "Lỗi không rõ"
            messagebox.showerror("Lỗi khi chạy chamthi.py", err)

    def add_result_text(self, text):
        def update():
            self.result_box.config(state='normal')
            self.result_box.insert(tk.END, text + "\n")
            self.result_box.see(tk.END)
            self.result_box.config(state='disabled')
        self.root.after(0, update)

    def notify_image_received(self, text="Đã nhận ảnh"):
        def update():
            self.result_box.config(state='normal')
            self.result_box.insert(tk.END, text + "\n")
            self.result_box.see(tk.END)
            self.result_box.config(state='disabled')
        self.root.after(0, update)


if __name__ == "__main__":
    root = tk.Tk()
    app_gui = AppGUI(root)  # Khởi tạo GUI, lưu tham chiếu toàn cục để Flask dùng

    flask_thread = FlaskThread(app)
    flask_thread.start()

    root.mainloop()
