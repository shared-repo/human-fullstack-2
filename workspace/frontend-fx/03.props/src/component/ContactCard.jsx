
function ContactCard(props) {

  const { contact } = props;
  return (
    <div>
      <p>이름: {contact.name}</p>
      <p>이메일: {contact.email}</p>
      <p>전화번호: {contact.phone}</p>
    </div>
  );
}

export default ContactCard;