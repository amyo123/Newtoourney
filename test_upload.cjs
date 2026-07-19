const fs = require('fs');
const FormData = require('form-data');
const fetch = require('node-fetch');

async function testUpload() {
  fs.writeFileSync('test_image.jpg', 'fake image data');
  const formData = new FormData();
  formData.append('file', fs.createReadStream('test_image.jpg'));

  const response = await fetch('https://verceldeploy-fawn-psi.vercel.app/api/upload', {
    method: 'POST',
    body: formData
  });

  const data = await response.json();
  console.log(data);
}
testUpload();
