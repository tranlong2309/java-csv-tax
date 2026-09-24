#!/bin/bash

echo "⏳ Đang tạo JavaDocs cho thư viện Shared CSV Tax..."

# Chạy lệnh maven để tạo javadoc (yêu cầu máy có cài maven)
mvn clean javadoc:javadoc -q

if [ $? -eq 0 ]; then
    echo "✅ JavaDocs đã được tạo thành công!"
    DOC_PATH="target/site/apidocs/index.html"
    echo "📂 Đường dẫn tài liệu: $DOC_PATH"
    
    # Mở tài liệu trên trình duyệt dựa trên hệ điều hành
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$DOC_PATH"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$DOC_PATH"
    elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
        start "$DOC_PATH"
    else
        echo "💡 Vui lòng mở thủ công file $DOC_PATH trên trình duyệt của bạn."
    fi
else
    echo "❌ Lỗi: Quá trình tạo JavaDocs thất bại!"
    exit 1
fi
