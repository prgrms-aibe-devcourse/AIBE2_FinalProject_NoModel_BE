import os
import sys
import json
import base64
from google import genai
from google.genai import types
from PIL import Image
from io import BytesIO

def load_api_key():
    """API 키를 환경변수 또는 .env 파일에서 로드"""
    # 먼저 환경변수에서 확인
    api_key = os.environ.get('GOOGLE_API_KEY')
    if api_key:
        return api_key
    
    # .env 파일에서 로드 시도
    try:
        env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../../../../../../../.env'))
        with open(env_path, 'r') as f:
            for line in f:
                if line.startswith('GOOGLE_API_KEY='):
                    api_key = line.split('=', 1)[1].strip()
                    # 환경변수에 설정 (중요: genai.Client() 호출 전에 반드시 설정)
                    os.environ['GOOGLE_API_KEY'] = api_key
                    return api_key
    except FileNotFoundError:
        pass
    
    raise ValueError("GOOGLE_API_KEY not found in environment variables or .env file")

def compose_images(product_image_path, model_image_path, custom_prompt, output_path):
    """이미지 합성 함수"""
    try:
        # API 키 로드 및 환경변수 설정
        api_key = load_api_key()
        
        # 클라이언트 생성 (환경변수 설정 후에 생성)
        client = genai.Client()
        
        # 이미지 로드
        product_image = Image.open(product_image_path)
        model_image = Image.open(model_image_path)
        
        # 프롬프트 설정 (커스텀 프롬프트가 없으면 기본 프롬프트 사용)
        if not custom_prompt or custom_prompt.strip() == "":
            text_input = "모델이 이 제품을 자연스럽게 광고하는 사진으로 바꿔줘"
        else:
            text_input = custom_prompt
        
        # Gemini API 호출
        response = client.models.generate_content(
            model="gemini-2.5-flash-image-preview",
            contents=[product_image, model_image, text_input],
        )
        
        # 응답 확인
        if not response.candidates:
            return {
                "success": False,
                "error": "No candidates returned from API"
            }
        
        if not response.candidates[0].content.parts:
            return {
                "success": False,
                "error": "No content parts in API response"
            }
        
        # 이미지 추출
        image_parts = [
            part.inline_data.data
            for part in response.candidates[0].content.parts
            if part.inline_data
        ]
        
        if image_parts:
            # 결과 이미지 저장
            image = Image.open(BytesIO(image_parts[0]))
            image.save(output_path)
            return {
                "success": True,
                "output_path": output_path,
                "message": "Image composition completed successfully"
            }
        else:
            return {
                "success": False,
                "error": "No image data found in API response"
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def main():
    """메인 함수 - 커맨드라인 인자를 처리"""
    if len(sys.argv) != 5:
        print(json.dumps({
            "success": False,
            "error": "Usage: python UseGeminiApi.py <product_image_path> <model_image_path> <custom_prompt> <output_path>"
        }))
        sys.exit(1)
    
    product_image_path = sys.argv[1]
    model_image_path = sys.argv[2]
    custom_prompt = sys.argv[3] if sys.argv[3] != "null" else ""
    output_path = sys.argv[4]
    
    # 입력 파일 존재 확인
    if not os.path.exists(product_image_path):
        print(json.dumps({
            "success": False,
            "error": f"Product image file not found: {product_image_path}"
        }))
        sys.exit(1)
    
    if not os.path.exists(model_image_path):
        print(json.dumps({
            "success": False,
            "error": f"Model image file not found: {model_image_path}"
        }))
        sys.exit(1)
    
    # 이미지 합성 실행
    result = compose_images(product_image_path, model_image_path, custom_prompt, output_path)
    
    # 결과를 JSON으로 출력
    print(json.dumps(result))

if __name__ == "__main__":
    main()
