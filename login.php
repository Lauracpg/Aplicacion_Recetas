<?php
header('Content-Type: application/json');
$servername = "db";
$username = "admin";
$password = "test";
$dbname = "database";

$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die(json_encode(["error" => "Connection failed"]));
}

$data = json_decode(file_get_contents("php://input"), true);
$email = $data['email'];
$password = $data['password'];

$sql = "SELECT id, password, nombre, email, foto FROM usuarios WHERE email=?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();
if($row = $result->fetch_assoc()){
    if(password_verify($password, $row['password'])){
        echo json_encode(["success" => true, "id" => $row['id'], "nombre" => $row['nombre'], "email" => $row['email'], "foto" => $row['foto']]);
    } else {
        echo json_encode(["success"=>false,"message"=>"Contraseña incorrecta"]);
    }
}else{
    echo json_encode(["success"=>false,"message"=>"Usuario no encontrado"]);
}
$stmt->close();
$conn->close();
?>
