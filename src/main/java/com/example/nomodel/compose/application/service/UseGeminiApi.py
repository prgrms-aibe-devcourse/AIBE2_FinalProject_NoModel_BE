import os
from google import genai
from google.genai import types
from PIL import Image
from io import BytesIO

# API키 가져오기(상대경로)
env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../../../../../../../.env'))

with open(env_path, 'r') as f:
    for line in f:
        if line.startswith('GOOGLE_API_KEY='):
            api_key = line.split('=', 1)[1].strip()
            os.environ['GOOGLE_API_KEY'] = api_key
            break


client = genai.Client()

product_image = Image.open('./test_product.png')
model_image = Image.open('./test_model.png')

text_input = """모델이 이 제품을 자연스럽게 광고하는 사진으로 바꿔줘"""

response = client.models.generate_content(
    model="gemini-2.5-flash-image-preview",
    contents=[product_image, model_image, text_input],
)

image_parts = [
    part.inline_data.data
    for part in response.candidates[0].content.parts
    if part.inline_data
]

if image_parts:
    image = Image.open(BytesIO(image_parts[0]))
    image.save('test_result.png')
    image.show()