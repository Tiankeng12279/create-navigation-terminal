"""从你的 Blockbench 原始导出生成两个方块模型。

规则：
  机身 = 导出里 group1(索引 0-20) + group3(索引 29-35)，原样保留；
  控件 = 下面 CONTROLS 表生成（唯一改这里，别的不会被带动）；
  反放 = 机身用「反放」导出，控件 y + DY。
用法：python tools/gen_nav_models.py
"""
import json, io, os, copy

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_FRONT = os.path.join(ROOT, "_backup", "authored_front.json")
SRC_TOP   = os.path.join(ROOT, "_backup", "authored_top.json")
OUT_DIR   = os.path.join(ROOT, "src/main/resources/assets/create_navigation_terminal/models/block")
DY        = 8.1488          # 反放相对正放的 y 偏移

BODY_KEEP = list(range(0, 21)) + list(range(29, 36))

# 控件规格：name, x0, x1, y0, y1, z0, z1, uv来源(0..#28 的某个元素索引), 说明
# z 越小越靠前（正面是 -z）；光驱外框凸 0.30，其余凸 0.18
CONTROLS = [
    ("slot_left",  3.60, 4.12, 5.73, 6.79, 0.25, 0.55, 27),
    ("slot_right", 8.26, 8.78, 5.73, 6.79, 0.25, 0.55, 27),
    ("slot_top",   4.12, 8.26, 6.53, 6.79, 0.25, 0.55, 27),
    ("slot_bot",   4.12, 8.26, 5.73, 6.00, 0.25, 0.55, 27),
    ("dpad_v",    11.01,11.61, 5.82, 7.50, 0.37, 0.55, 21),
    ("dpad_h",    10.50,12.14, 6.37, 6.97, 0.37, 0.55, 21),
    ("btn_green",  9.55,10.16, 6.70, 7.32, 0.37, 0.55, 23),
    ("btn_orange", 9.64,10.16, 5.82, 6.35, 0.37, 0.55, 24),
]

def build(src, dst, dy):
    d = json.load(io.open(src, encoding="utf-8"))
    els = d["elements"]
    body = [copy.deepcopy(els[i]) for i in BODY_KEEP]
    ctrl = []
    for name, x0, x1, y0, y1, z0, z1, uvsrc in CONTROLS:
        src_el = copy.deepcopy(els[uvsrc])
        src_el.pop("rotation", None)
        src_el["from"] = [round(x0, 4), round(y0 + dy, 4), round(z0, 4)]
        src_el["to"]   = [round(x1, 4), round(y1 + dy, 4), round(z1, 4)]
        ctrl.append(src_el)
    out = {
        "loader": "create_navigation_terminal:rotated_cuboids",
        "textures": {"0": "create_navigation_terminal:block/navigation_terminal",
                     "particle": "create_navigation_terminal:block/navigation_terminal"},
        "display": d.get("display", {}),
        "cn_elements": body + ctrl,
        "cn_skip": [],
    }
    # 校验：不能有 from > to
    for i, e in enumerate(out["cn_elements"]):
        for k in range(3):
            assert e["from"][k] <= e["to"][k], f"{dst} 元素 {i} 轴{k} from>to"
    io.open(dst, "w", encoding="utf-8", newline="\n").write(
        json.dumps(out, ensure_ascii=False, indent=2) + "\n")
    print(f"{os.path.basename(dst):32s} 机身 {len(body)} + 控件 {len(ctrl)} = {len(out['cn_elements'])}")

import sys
build(SRC_FRONT, os.path.join(OUT_DIR, "navigation_terminal.json"), 0.0)
if "--both" in sys.argv:
    build(SRC_TOP, os.path.join(OUT_DIR, "navigation_terminal_inverted.json"), DY)
else:
    print("跳过反放模型（默认不覆盖；要一起生成请加 --both）")
