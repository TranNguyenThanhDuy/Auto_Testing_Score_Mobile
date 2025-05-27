import tkinter as tk
from tkinter import filedialog, messagebox
import subprocess
import threading
import os

def browse_answers():
    filename = filedialog.askopenfilename(filetypes=[("JSON files", "*.json")])
    if filename:
        answers_path.set(filename)

def run_grading_thread():
    t = threading.Thread(target=run_grading)
    t.start()

def run_grading():
    answers_file = answers_path.get()
    csv_file = output_csv.get()

    if not answers_file:
        messagebox.showwarning("Thiếu file", "Vui lòng chọn file answers.json!")
        return
    if not csv_file:
        messagebox.showwarning("Thiếu tên CSV", "Vui lòng nhập tên file CSV!")
        return
    if not os.path.isfile("chamthi.py"):
        messagebox.showerror("Lỗi", "Không tìm thấy file 'chamthi.py' trong thư mục hiện tại!")
        return

    try:
        completed = subprocess.run(
            ["python", "chamthi.py", "--answers", answers_file, "--output", csv_file],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore',
            check=True
        )
        output = completed.stdout or ""
        output = filter_output(output)
        root.after(0, update_result_box, output)
    except subprocess.CalledProcessError as e:
        err = e.stderr or "Lỗi không rõ"
        root.after(0, lambda: messagebox.showerror("Lỗi khi chạy chamthi.py", err))

def filter_output(raw_output):
    lines = raw_output.splitlines()
    sbd = ""
    diem = ""
    for line in lines:
        if line.startswith("SBD:"):
            sbd = line.strip()
        elif line.startswith("Điểm:"):
            diem = line.strip()
    # Ghép ngang
    return f"{sbd} | {diem}" if sbd or diem else raw_output

def update_result_box(text):
    result_box.config(state='normal')
    result_box.insert(tk.END, text + "\n")
    result_box.see(tk.END)
    result_box.config(state='disabled')

root = tk.Tk()
root.title("Chấm điểm GUI (không MQTT)")
root.geometry("600x350")

answers_path = tk.StringVar()
output_csv = tk.StringVar(value="grading_result.csv")

tk.Label(root, text="Chọn file đáp án (answers.json):").pack(anchor='w', padx=10, pady=5)
frame_ans = tk.Frame(root)
frame_ans.pack(fill='x', padx=10)
tk.Entry(frame_ans, textvariable=answers_path).pack(side='left', fill='x', expand=True)
tk.Button(frame_ans, text="Chọn...", command=browse_answers).pack(side='left', padx=5)

tk.Label(root, text="Tên file CSV lưu kết quả:").pack(anchor='w', padx=10, pady=5)
tk.Entry(root, textvariable=output_csv).pack(fill='x', padx=10)

tk.Button(root, text="Chấm điểm", command=run_grading_thread).pack(pady=15)

result_box = tk.Text(root, height=10, font=("Courier", 11))
result_box.pack(fill='both', padx=10, pady=10)
result_box.insert(tk.END, "📋 Kết quả chấm điểm sẽ hiện ở đây...\n")
result_box.config(state='disabled')

root.mainloop()
